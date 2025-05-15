import java.time.LocalDate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import models.Cliente;  // si es tu clase propia

public class TestWhatsApp {
    public static void main(String[] args) {
        WebDriver driver = null;
        try {
            // 1. Configuración del WebDriver
            System.setProperty("webdriver.chrome.driver", "C:/driver/chromedriver.exe");

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-notifications");

            driver = new ChromeDriver(options);

            // 2. Creación del cliente de prueba
            Cliente testCliente = new Cliente(
                    "Test Users",
                    "+593968822603",
                    LocalDate.now().plusDays(2)
            );

            System.out.println("Iniciando prueba con cliente: " + testCliente.getNombres());

            // 3. Envío de mensaje de prueba directo
            String telefono = "593963353926"; // sin el "+" para WhatsApp Web
            String mensaje = "Hola " + testCliente.getNombres() + ", este es un mensaje de prueba desde el sistema creado por Mancar Software.";

            enviarMensajePrueba(driver, telefono, mensaje);

            System.out.println("Prueba completada exitosamente");

            // Esperar 5 segundos antes de cerrar el navegador
            Thread.sleep(5000);

            // Cerrar el navegador manualmente al final de la prueba
            driver.quit();

        } catch (Exception e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static void enviarMensajePrueba(WebDriver driver, String telefono, String mensaje) {
        try {
            // 1. Asegurar que esté logueado
            driver.get("https://web.whatsapp.com/");
            System.out.println("Escanea el código QR si es necesario...");
            Thread.sleep(15000); // Espera para escanear QR

            // 2. Generar URL con mensaje
            String url = "https://web.whatsapp.com/send?phone=" + telefono +
                    "&text=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

            driver.get(url);
            System.out.println("Abriendo chat con mensaje...");

            // 3. Esperar que aparezca el campo de texto
            WebDriverWait wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(30));
            WebElement chatBox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[@contenteditable='true' and @data-tab='10']")));

            // 4. Esperar que el botón de enviar esté disponible
            Thread.sleep(3000); // Espera adicional

            // 5. Enviar el mensaje
            chatBox.sendKeys(Keys.ENTER);

            System.out.println("Mensaje enviado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al enviar mensaje de prueba: " + e.getMessage());
        }
    }
}
