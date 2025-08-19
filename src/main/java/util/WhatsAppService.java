package util;

import models.Cliente;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WhatsAppService {
        private static final String RUTA_CHROME_DRIVER = "C:/driver/chromedriver.exe";
        private static final String USER_DATA_DIR = "C:/whatsapp_session";
        private static final int LIMITE_DIARIO = 80;
        private static final LocalTime HORARIO_INICIO = LocalTime.of(9, 0);
        private static final LocalTime HORARIO_FIN = LocalTime.of(21, 0);
        private static final Object DB_LOCK = new Object();
        private static WebDriver driver = null;

        static {
                // Configuración inicial al cargar la clase
                System.setProperty("webdriver.chrome.driver", RUTA_CHROME_DRIVER);
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

        public static void enviarAlertaPersonalizada(Cliente cliente, String tipoAlerta) {
                if (!validarCondicionesEnvio()) {
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

                        AuditoriaUtil.registrar(
                                SessionManager.getUsuarioActual() != null ? SessionManager.getUsuarioActual().getNombre() : "SISTEMA",
                                "SEND_MESSAGE",
                                "WHATSAPP",
                                null,
                                tipoAlerta + " -> " + cliente.getTelefono()
                        );

                        System.out.println(LocalDateTime.now() + " - Alerta (" + tipoAlerta + ") enviada");
                        pausaAleatoria();

                } catch (Exception e) {
                        System.err.println(LocalDateTime.now() + " - Error enviando " + tipoAlerta + ": " + e.getMessage());
                        reiniciarDriverCompleto();
                }
        }

        private static void crearDirectorioSesion() {
                File directorio = new File(USER_DATA_DIR);
                if (!directorio.exists()) {
                        if (directorio.mkdirs()) {
                                System.out.println("Directorio de sesión creado: " + USER_DATA_DIR);
                        } else {
                                System.err.println("Error al crear directorio de sesión");
                        }
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
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--user-data-dir=" + USER_DATA_DIR);
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
}