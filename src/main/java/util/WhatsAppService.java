package util;

import models.Cliente;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class WhatsAppService {
        private static final String DRIVER_PROPERTY = "control_gimnasio.chromedriver";
        private static final String DRIVER_ENV = "CONTROL_GIMNASIO_CHROMEDRIVER";
        private static final String SESSION_PROPERTY = "control_gimnasio.whatsappSession";
        private static final String SESSION_ENV = "CONTROL_GIMNASIO_WHATSAPP_SESSION";
        private static final String DRIVER_FOLDER = "driver";
        private static final String SESSION_FOLDER = "whatsapp_session";
        private static final int LIMITE_DIARIO = 80;
        private static final int DIRECTORIO_BUSQUEDA_MAX = 4;
        private static final LocalTime HORARIO_INICIO = LocalTime.of(9, 0);
        private static final LocalTime HORARIO_FIN = LocalTime.of(23, 0);
        private static final Object DB_LOCK = new Object();
        private static final String CHROMEDRIVER_NOMBRE = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "chromedriver.exe"
                : "chromedriver";
        private static final Path CHROMEDRIVER_PATH = resolveChromeDriverPath();
        private static final Path SESSION_DIRECTORY = resolveSessionDirectory();

        private static WebDriver driver = null;
        private static boolean driverAdvertido;

        static {
                if (CHROMEDRIVER_PATH != null) {
                        System.setProperty("webdriver.chrome.driver", CHROMEDRIVER_PATH.toString());
                } else {
                        driverAdvertido = true;
                        System.err.println("⚠️ No se encontró chromedriver. Coloca el ejecutable en una carpeta '" + DRIVER_FOLDER +
                                "' junto a la aplicación o define la variable CONTROL_GIMNASIO_CHROMEDRIVER.");
                }
                System.setProperty("webdriver.http.factory", "jdk-http-client");
                crearDirectorioSesion();
        }

        public static void enviarAlerta(Cliente cliente) {
                enviarAlertaPersonalizada(cliente, "Vencimiento");
        }

        public static void enviarAlertaRegistro(Cliente cliente) {
                enviarAlertaPersonalizada(cliente, "Registro");
        }

        public static void enviarAlertaRenovacion(Cliente cliente) {
                enviarAlertaPersonalizada(cliente, "Renovación");
        }

        /**
         * Envía un aviso de que la membresía del cliente está próxima a vencer.
         * @param cliente Cliente a notificar
         */
        public static void enviarAvisoVencimiento(Cliente cliente) {
                enviarAlertaPersonalizada(cliente, "Vencimiento");
        }

        private static void enviarAlertaPersonalizada(Cliente cliente, String tipoAlerta) {
                if (!chromedriverDisponible()) {
                        return;
                }
                if (!validarCondicionesEnvio()) {
                        return;
                }

                if (cliente.getTelefonoVisible() == null || cliente.getTelefonoVisible().isBlank() || cliente.getTelefonoVisible().matches("0{10}")) {
                        System.out.println(LocalDateTime.now() + " - Cliente sin número válido, se omite envío.");
                        return;
                }

                try {
                        if (driver == null || !esDriverActivo()) {
                                iniciarDriver();
                        }

                        String telefonoNormalizado = normalizarTelefono(cliente.getTelefono());
                        if (!validarFormatoTelefono(telefonoNormalizado)) {
                                System.err.println(LocalDateTime.now() + " - Formato inválido: " + cliente.getTelefono());
                                return;
                        }

                        String mensaje = construirMensajePersonalizado(cliente, tipoAlerta);
                        String url = generarUrlWhatsApp(telefonoNormalizado, mensaje);

                        System.out.println(LocalDateTime.now() + " - Intentando enviar (" + tipoAlerta + "): " + telefonoNormalizado);
                        driver.get(url);

                        if (!manejarPantallaInvitacion(driver)) {
                                System.err.println(LocalDateTime.now() + " - No se pudo iniciar el chat");
                                return;
                        }

                        if (!esperarCampoChat(driver)) {
                                throw new RuntimeException("Tiempo de conexión agotado");
                        }

                        enviarMensaje(mensaje);

                        synchronized (DB_LOCK) {
                                registrarEnvioExitoso(cliente, tipoAlerta);
                        }

                        System.out.println(LocalDateTime.now() + " - Alerta (" + tipoAlerta + ") enviada");
                        pausaAleatoria();

                } catch (Exception e) {
                        System.err.println(LocalDateTime.now() + " - Error enviando " + tipoAlerta + ": " + e.getMessage());
                        reiniciarDriverCompleto();
                }
        }

        private static void crearDirectorioSesion() {
                if (SESSION_DIRECTORY == null) {
                        return;
                }
                try {
                        Files.createDirectories(SESSION_DIRECTORY);
                } catch (Exception e) {
                        System.err.println("Error al preparar la sesión de WhatsApp en " + SESSION_DIRECTORY + ": " + e.getMessage());
                }
        }

        private static boolean esDriverActivo() {
                try {
                        driver.getCurrentUrl();
                        return true;
                } catch (Exception e) {
                        return false;
                }
        }

        private static void reiniciarDriverCompleto() {
                cerrarDriver();
                try {
                        Thread.sleep(2000); // Esperar antes de reintentar
                        iniciarDriver();
                } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                }
        }

        private static boolean manejarPantallaInvitacion(WebDriver driver) {
                try {
                        By inviteButtonLocator = By.xpath("//div[@role='button' and contains(., 'Enviar invitación')]");
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        WebElement inviteButton = wait.until(ExpectedConditions.visibilityOfElementLocated(inviteButtonLocator));
                        System.out.println(LocalDateTime.now() + " - Iniciando chat con nuevo contacto...");
                        inviteButton.click();
                        wait.until(ExpectedConditions.invisibilityOfElementLocated(inviteButtonLocator));
                        return true;
                } catch (TimeoutException e) {
                        return true;
                }
        }

        private static String normalizarTelefono(String telefono) {
                String digits = telefono.replaceAll("\\D+", "");
                while (digits.startsWith("0")) {
                        digits = digits.substring(1);
                }
                if (!digits.startsWith("593") && digits.length() == 9) {
                        digits = "593" + digits;
                }
                return digits;
        }

        private static boolean validarFormatoTelefono(String telefono) {
                return telefono.matches("^[1-9]\\d{11,14}$");
        }

        private static void iniciarDriver() {
                if (!chromedriverDisponible()) {
                        return;
                }
                ChromeOptions options = new ChromeOptions();
                if (SESSION_DIRECTORY != null) {
                        options.addArguments("--user-data-dir=" + SESSION_DIRECTORY.toString());
                }
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--remote-debugging-port=9222");
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-extensions");
                options.addArguments("--disable-infobars");
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-browser-side-navigation");
                options.addArguments("--disable-features=VizDisplayCompositor");
                options.addArguments("--disable-software-rasterizer");
                options.addArguments("--log-level=3");
                options.addArguments("--silent");
                options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

                try {
                        driver = new ChromeDriver(options);
                        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
                        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
                        System.out.println(LocalDateTime.now() + " - ChromeDriver iniciado exitosamente");
                } catch (SessionNotCreatedException e) {
                        System.err.println(LocalDateTime.now() + " - ERROR CRÍTICO: No se pudo iniciar ChromeDriver");
                        System.err.println("Posibles soluciones:");
                        System.err.println("1. Verifique que Chrome esté actualizado");
                        System.err.println("2. Asegúrese que chromedriver.exe coincide con la versión de Chrome");
                        System.err.println("3. Cierre todas las instancias de Chrome antes de ejecutar");
                        throw new RuntimeException("Error fatal al iniciar ChromeDriver", e);
                }
        }

        private static boolean validarCondicionesEnvio() {
                if (!esHorarioLaboral()) {
                        System.out.println("⚠️ Fuera de horario (9AM-9PM)");
                        return false;
                }
                if (limiteDiarioAlcanzado()) {
                        System.out.println("⚠️ Límite diario alcanzado (" + LIMITE_DIARIO + ")");
                        return false;
                }
                return true;
        }

        private static boolean esHorarioLaboral() {
                LocalTime ahora = LocalTime.now();
                return !ahora.isBefore(HORARIO_INICIO) && !ahora.isAfter(HORARIO_FIN);
        }

        private static boolean limiteDiarioAlcanzado() {
                String sql = "SELECT COUNT(*) FROM alertas_enviadas WHERE fecha_envio = ?";
                try (Connection conn = DatabaseUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, LocalDate.now().toString());
                        try (ResultSet rs = stmt.executeQuery()) {
                                return rs.next() && rs.getInt(1) >= LIMITE_DIARIO;
                        }
                } catch (SQLException e) {
                        System.err.println("Error verificando límite: " + e.getMessage());
                        return true;
                }
        }

        private static String construirMensajePersonalizado(Cliente cliente, String tipoAlerta) throws SQLException {
                String campo = "";
                switch (tipoAlerta) {
                        case "Vencimiento": campo = "mensaje_whatsapp"; break;
                        case "Registro": campo = "mensaje_registro"; break;
                        case "Renovación": campo = "mensaje_renovacion"; break;
                        default: campo = "mensaje_whatsapp";
                }

                String sql = "SELECT " + campo + " FROM config WHERE id = 1";
                try (Connection conn = DatabaseUtil.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                        String plantilla = rs.getString(campo);
                        LocalDate fechaVenc = cliente.getFecha_vencimientoDate();
                        String fechaFormateada = fechaVenc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        if ("Vencimiento".equals(tipoAlerta)) {
                                long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVenc);
                                return plantilla.replace("[NOMBRE]", cliente.getNombres())
                                        .replace("[APELLIDO]", cliente.getApellidos())
                                        .replace("[GIMNASIO]", obtenerNombreGimnasio())
                                        .replace("[FECHA]", fechaFormateada)
                                        .replace("[DIAS]", String.valueOf(dias))
                                        .replace("[LINK]", obtenerLinkPago());
                        } else {
                                return plantilla.replace("[NOMBRE]", cliente.getNombres())
                                        .replace("[APELLIDO]", cliente.getApellidos())
                                        .replace("[GIMNASIO]", obtenerNombreGimnasio())
                                        .replace("[MEMBRESIA]", cliente.getTipoMembresia())
                                        .replace("[FECHA]", fechaFormateada);
                        }
                }
        }

        private static void registrarEnvioExitoso(Cliente cliente, String tipoAlerta) {
                String sql = "INSERT INTO alertas_enviadas (telefono_cliente, fecha_envio, tipo_alerta) VALUES (?, ?, ?)";
                try {
                        DatabaseUtil.executeUpdate(sql,
                                cliente.getTelefono(),
                                LocalDate.now().toString(),
                                tipoAlerta);
                } catch (Exception e) {
                        System.err.println("Error registrando envío: " + e.getMessage());
                }
        }

        private static String obtenerNombreGimnasio() throws SQLException {
                try (Connection conn = DatabaseUtil.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT nombre_gimnasio FROM config WHERE id = 1")) {
                        return rs.next() ? rs.getString("nombre_gimnasio") : "Dioses del Olimpo";
                }
        }

        private static String obtenerLinkPago() {
                return "https://pago.gimnasio.com";
        }

        private static String generarUrlWhatsApp(String telefono, String mensaje) {
                return "https://web.whatsapp.com/send?phone=" + telefono +
                        "&text=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        }

        private static boolean esperarCampoChat(WebDriver driver) {
                try {
                        By chatBoxLocator = By.cssSelector("div[contenteditable='true'][data-tab]");
                        new WebDriverWait(driver, Duration.ofSeconds(90))
                                .until(ExpectedConditions.visibilityOfElementLocated(chatBoxLocator));
                        return true;
                } catch (TimeoutException e) {
                        System.out.println("⚠️ No se encontró el campo de chat");
                        return false;
                }
        }

        private static void enviarMensaje(String mensaje) {
                try {
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
                        WebElement inputBox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//div[@contenteditable='true' and @data-tab='10']")
                        ));

                        inputBox.click();
                        inputBox.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                        inputBox.sendKeys(Keys.BACK_SPACE);
                        inputBox.sendKeys(mensaje);
                        Thread.sleep(300);
                        inputBox.sendKeys(Keys.ENTER);

                        System.out.println(LocalDateTime.now() + " - Mensaje enviado automáticamente");
                } catch (Exception e) {
                        System.err.println(LocalDateTime.now() + " - Error enviando mensaje: " + e.getMessage());
                }
        }

        private static void pausaAleatoria() throws InterruptedException {
                Thread.sleep(5000 + (long) (Math.random() * 10000));
        }

        public static void cerrarDriver() {
                if (driver != null) {
                        try {
                                driver.quit();
                                System.out.println(LocalDateTime.now() + " - ChromeDriver cerrado correctamente");
                        } catch (Exception e) {
                                System.err.println("Error al cerrar el driver: " + e.getMessage());
                        }
                        driver = null;
                }
        }

        private static boolean chromedriverDisponible() {
                if (CHROMEDRIVER_PATH != null && Files.exists(CHROMEDRIVER_PATH)) {
                        return true;
                }
                if (!driverAdvertido) {
                        driverAdvertido = true;
                        System.err.println("⚠️ ChromeDriver no está configurado. Asegúrate de colocar el ejecutable en '" + DRIVER_FOLDER +
                                "' o define CONTROL_GIMNASIO_CHROMEDRIVER/" + DRIVER_PROPERTY + ".");
                }
                return false;
        }

        private static Path resolveChromeDriverPath() {
                Path propertyPath = getExistingPath(System.getProperty(DRIVER_PROPERTY));
                if (propertyPath != null) {
                        return propertyPath;
                }
                Path envPath = getExistingPath(System.getenv(DRIVER_ENV));
                if (envPath != null) {
                        return envPath;
                }
                Path webdriverProperty = getExistingPath(System.getProperty("webdriver.chrome.driver"));
                if (webdriverProperty != null) {
                        return webdriverProperty;
                }

                Path workingDir = buscarDriverEnDirectorios(Paths.get(""));
                if (workingDir != null) {
                        return workingDir;
                }

                Path appDir = buscarDriverEnDirectorios(obtenerDirectorioAplicacion());
                if (appDir != null) {
                        return appDir;
                }

                Path homeDir = buscarDriverEnDirectorios(Paths.get(System.getProperty("user.home", "")));
                if (homeDir != null) {
                        return homeDir;
                }

                Path homeControl = getExistingPath(Paths.get(System.getProperty("user.home", ""), "ControlGimnasio", DRIVER_FOLDER, CHROMEDRIVER_NOMBRE).toString());
                if (homeControl != null) {
                        return homeControl;
                }

                if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                        Path linuxDefault = getExistingPath("/usr/bin/" + CHROMEDRIVER_NOMBRE);
                        if (linuxDefault != null) {
                                return linuxDefault;
                        }
                }

                Path fallback = getExistingPath(Paths.get("C:/driver", CHROMEDRIVER_NOMBRE).toString());
                return fallback;
        }

        private static Path resolveSessionDirectory() {
                Path customProperty = getPath(System.getProperty(SESSION_PROPERTY));
                if (customProperty != null) {
                        return customProperty;
                }
                Path envPath = getPath(System.getenv(SESSION_ENV));
                if (envPath != null) {
                        return envPath;
                }

                String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (osName.contains("win")) {
                        String localAppData = System.getenv("LOCALAPPDATA");
                        if (localAppData != null && !localAppData.isBlank()) {
                                return Paths.get(localAppData, "ControlGimnasio", SESSION_FOLDER);
                        }
                        return Paths.get(System.getProperty("user.home", ""), "AppData", "Local", "ControlGimnasio", SESSION_FOLDER);
                }
                return Paths.get(System.getProperty("user.home", ""), ".control_gimnasio", SESSION_FOLDER);
        }

        private static Path getExistingPath(String value) {
                Path path = getPath(value);
                if (path != null && Files.exists(path)) {
                        return path.toAbsolutePath().normalize();
                }
                return null;
        }

        private static Path getPath(String value) {
                if (value == null || value.isBlank()) {
                        return null;
                }
                try {
                        return Paths.get(value.trim()).toAbsolutePath().normalize();
                } catch (Exception e) {
                        return null;
                }
        }

        private static Path buscarDriverEnDirectorios(Path inicio) {
                if (inicio == null) {
                        return null;
                }
                Path current = inicio.toAbsolutePath().normalize();
                for (int i = 0; i < DIRECTORIO_BUSQUEDA_MAX && current != null; i++) {
                        Path candidate = current.resolve(DRIVER_FOLDER).resolve(CHROMEDRIVER_NOMBRE);
                        if (Files.exists(candidate)) {
                                return candidate.toAbsolutePath().normalize();
                        }
                        current = current.getParent();
                }
                return null;
        }

        private static Path obtenerDirectorioAplicacion() {
                try {
                        Path location = Paths.get(WhatsAppService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                        if (Files.isRegularFile(location)) {
                                return location.getParent();
                        }
                        return location;
                } catch (Exception e) {
                        return null;
                }
        }
}
