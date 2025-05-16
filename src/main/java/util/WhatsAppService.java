package util;

import models.Cliente;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
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

        public static void enviarAlerta(Cliente cliente) {
                if (!validarCondicionesEnvio()) {
                        return;
                }

                ChromeOptions options = new ChromeOptions();
                configurarOpcionesChrome(options);

                WebDriver driver = new ChromeDriver(options);
                try {
                        String mensaje = construirMensajePersonalizado(cliente);
                        String url = generarUrlWhatsApp(cliente.getTelefono(), mensaje);

                        driver.get("https://web.whatsapp.com/");
                        if (!esperarConexion(driver)) {
                                throw new RuntimeException("Tiempo de conexión agotado");
                        }

                        enviarMensaje(driver);

                        synchronized (DB_LOCK) {
                                registrarEnvioExitoso(cliente);
                        }

                        pausaAleatoria();

                } catch (Exception e) {
                        System.err.println("Error enviando a " + cliente.getTelefono() + ": " + e.getMessage());
                } finally {
                        driver.quit();
                }
        }

        private static void configurarOpcionesChrome(ChromeOptions options) {
                options.addArguments("--headless");
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
                        LocalDate fechaVenc = cliente.getFechaVencimientoDate();

                        long dias = ChronoUnit.DAYS.between(LocalDate.now(), fechaVenc);
                        String fechaFormateada = fechaVenc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        return plantilla.replace("[NOMBRE]", cliente.getNombres())
                                .replace("[GIMNASIO]", obtenerNombreGimnasio())
                                .replace("[FECHA]", fechaFormateada)
                                .replace("[DIAS]", String.valueOf(dias))
                                .replace("[LINK]", obtenerLinkPago());
                }
        }

        private static void registrarEnvioExitoso(Cliente cliente) {
                String sql = "INSERT INTO alertas_enviadas (telefono_cliente, fecha_envio, tipo_alerta) VALUES (?, ?, ?)";
                try {
                        LocalDate fechaVenc = cliente.getFechaVencimientoDate();
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

        private static String obtenerLinkPago() throws SQLException {
                return "https://pago.gimnasio.com"; // Puedes modificarlo para obtenerlo de la BD si es necesario
        }

        private static String generarUrlWhatsApp(String telefono, String mensaje) {
                return "https://web.whatsapp.com/send?phone=" + telefono +
                        "&text=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        }

        private static boolean esperarConexion(WebDriver driver) {
                try {
                        new WebDriverWait(driver, Duration.ofSeconds(120))
                                .until(ExpectedConditions.visibilityOfElementLocated(
                                        By.xpath("//div[@contenteditable='true']")));
                        return true;
                } catch (TimeoutException e) {
                        return false;
                }
        }

        private static void enviarMensaje(WebDriver driver) {
                WebElement chatBox = driver.findElement(By.xpath("//div[@contenteditable='true']"));
                chatBox.sendKeys(Keys.ENTER);
        }

        private static void pausaAleatoria() throws InterruptedException {
                Thread.sleep(5000 + (long)(Math.random() * 10000));
        }
}