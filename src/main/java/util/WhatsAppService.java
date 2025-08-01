package util;

import models.Cliente;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WhatsAppService {
        private static final String RUTA_CHROME_DRIVER = "C:/driver/chromedriver.exe";
        private static final int LIMITE_DIARIO = 80;
        private static final LocalTime HORARIO_INICIO = LocalTime.of(9, 0);
        private static final LocalTime HORARIO_FIN = LocalTime.of(21, 0);
        private static final Object DB_LOCK = new Object();
        private static WebDriver driver = null;

        public static void enviarAlerta(Cliente cliente) {
                if (!validarCondicionesEnvio()) {
                        return;
                }

                if (driver == null) {
                        iniciarDriver();
                }

                try {
                        String telefonoNormalizado = normalizarTelefono(cliente.getTelefono());

                        if (!validarFormatoTelefono(telefonoNormalizado)) {
                                System.err.println(LocalDateTime.now() + " - Formato inválido: " + cliente.getTelefono() + " -> " + telefonoNormalizado);
                                return;
                        }

                        String mensaje = construirMensajePersonalizado(cliente);
                        String url = generarUrlWhatsApp(telefonoNormalizado, mensaje);

                        System.out.println(LocalDateTime.now() + " - Intentando enviar a: " + telefonoNormalizado);
                        driver.get(url);

                        if (!manejarPantallaInvitacion(driver)) {
                                System.err.println(LocalDateTime.now() + " - No se pudo iniciar el chat con: " + telefonoNormalizado);
                                return;
                        }

                        if (!esperarCampoChat(driver)) {
                                throw new RuntimeException("Tiempo de conexión agotado");
                        }

                        enviarMensaje(telefonoNormalizado, mensaje);

                        synchronized (DB_LOCK) {
                                registrarEnvioExitoso(cliente);
                        }

                        System.out.println(LocalDateTime.now() + " - Alerta enviada a: " + telefonoNormalizado + " - " + cliente.getNombres());

                        pausaAleatoria();

                } catch (Exception e) {
                        System.err.println(LocalDateTime.now() + " - Error enviando a " + cliente.getTelefono() + ": " + e.getMessage());
                        if (driver != null) {
                                driver.quit();
                                driver = null;
                        }
                }
        }

        private static boolean manejarPantallaInvitacion(WebDriver driver) {
                try {
                        By inviteButtonLocator = By.xpath("//div[@role='button' and contains(., 'Enviar invitación')]");
                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        WebElement inviteButton = wait.until(ExpectedConditions.visibilityOfElementLocated(inviteButtonLocator));
                        System.out.println(LocalDateTime.now() + " - Número no en contactos. Iniciando chat...");
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
                options.addArguments("--user-data-dir=C:/whatsapp_session");
                configurarOpcionesChrome(options);
                driver = new ChromeDriver(options);
        }

        private static void configurarOpcionesChrome(ChromeOptions options) {
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--window-size=1920,1080");
                System.setProperty("webdriver.chrome.driver", RUTA_CHROME_DRIVER);
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

        private static String construirMensajePersonalizado(Cliente cliente) throws SQLException {
                String sql = "SELECT mensaje_whatsapp FROM config WHERE id = 1";
                try (Connection conn = DatabaseUtil.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                        String plantilla = rs.getString("mensaje_whatsapp");
                        LocalDate fechaVenc = cliente.getFecha_vencimientoDate();

                        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVenc);
                        String fechaFormateada = fechaVenc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        return plantilla.replace("[NOMBRE]", cliente.getNombres())
                                .replace("[APELLIDO]", cliente.getApellidos())
                                .replace("[GIMNASIO]", obtenerNombreGimnasio())
                                .replace("[FECHA]", fechaFormateada)
                                .replace("[DIAS]", String.valueOf(dias))
                                .replace("[LINK]", obtenerLinkPago());
                }
        }

        private static void registrarEnvioExitoso(Cliente cliente) {
                String sql = "INSERT INTO alertas_enviadas (telefono_cliente, fecha_envio, tipo_alerta) VALUES (?, ?, ?)";
                try {
                        LocalDate fechaVenc = cliente.getFecha_vencimientoDate();
                        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVenc);

                        DatabaseUtil.executeUpdate(sql,
                                cliente.getTelefono(),
                                LocalDate.now().toString(),
                                "Alerta a " + dias + " días");

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
                        System.out.println("⚠️ No se encontró el campo de chat. URL: " + driver.getCurrentUrl());
                        System.out.println("⚠️ HTML parcial: " + driver.getPageSource().substring(0, 500));
                        return false;
                }
        }

        public static void enviarMensaje(String numero, String mensaje) {
                try {
                        String url = "https://web.whatsapp.com/send?phone=" + numero;
                        driver.get(url);

                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

                        // Espera a que el campo de entrada esté listo
                        // Esperar a que el input esté disponible
                        WebElement inputBox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.xpath("//div[@contenteditable='true' and @data-tab='10']")
                        ));

// Limpiar el campo antes de escribir (Ctrl+A + Delete)
                        inputBox.click();
                        inputBox.sendKeys(Keys.chord(Keys.CONTROL, "a")); // Selecciona todo
                        inputBox.sendKeys(Keys.BACK_SPACE);              // Elimina lo seleccionado

// Escribir y enviar mensaje
                        inputBox.sendKeys(mensaje); // Solo una vez
                        Thread.sleep(300);          // Espera mínima para evitar errores
                        inputBox.sendKeys(Keys.ENTER);


                        System.out.println(LocalDateTime.now() + " - Mensaje enviado automáticamente a: " + numero);
                } catch (Exception e) {
                        System.err.println(LocalDateTime.now() + " - Error enviando automáticamente a " + numero + ": " + e.getMessage());
                }
        }


        private static void pausaAleatoria() throws InterruptedException {
                Thread.sleep(5000 + (long) (Math.random() * 10000));
        }

        public static void cerrarDriver() {
                if (driver != null) {
                        driver.quit();
                        driver = null;
                }
        }
}
