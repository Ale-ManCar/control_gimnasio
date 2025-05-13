package util;

import models.Cliente;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

public class WhatsAppService {
        // ============ CONFIGURAR AL VENDER ============
        private static final String RUTA_CHROME_DRIVER = "C:/driver/chromedriver.exe";
        private static final String NUMERO_GIMNASIO = "+593968822603"; // Número asociado al WhatsApp
        private static final String MENSAJE_BASE = """
    ¡Hola [NOMBRE]! Tu membresía en *[GIMNASIO]* está por vencer.
    *Fecha de vencimiento:* [FECHA]
    Renueva aquí: [LINK]""";
        // ==============================================

        public static void enviarAlerta(Cliente cliente) {
                System.setProperty("webdriver.chrome.driver", RUTA_CHROME_DRIVER);
                WebDriver driver = new ChromeDriver();

                try {
                        String mensaje = construirMensaje(cliente);
                        String url = "https://web.whatsapp.com/send?phone=" + cliente.getTelefono() + "&text=" +
                                URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

                        driver.get(url);
                        esperarConexion(driver);

                        // Enviar mensaje
                        WebElement input = driver.findElement(By.xpath("//div[@contenteditable='true']"));
                        input.sendKeys(Keys.ENTER);

                        System.out.println("Mensaje enviado a " + cliente.getNombres());

                } catch (Exception e) {
                        System.err.println("Error al enviar: " + e.getMessage());
                } finally {
                        driver.quit();
                }
        }

        private static void esperarConexion(WebDriver driver) {
                try {
                        // Espera máxima de 2 minutos para escanear QR
                        new WebDriverWait(driver, Duration.ofSeconds(120))
                                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@contenteditable='true']")));
                } catch (TimeoutException e) {
                        throw new RuntimeException("Tiempo de espera agotado. ¿Escaneaste el QR?");
                }
        }

        private static String construirMensaje(Cliente cliente) {
                return MENSAJE_BASE
                        .replace("[NOMBRE]", cliente.getNombres())
                        .replace("[GIMNASIO]", "Mi Gimnasio") // CONFIGURAR
                        .replace("[FECHA]", cliente.getFechaVencimiento().toString())
                        .replace("[LINK]", "https://pago.gimnasio.com"); // CONFIGURAR
        }
}