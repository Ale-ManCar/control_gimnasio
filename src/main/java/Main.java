import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import util.LicenseManager;
import util.HardwareUtil;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Modo debug para desarrollo
        if (isDebugMode()) {
            showDebugInfo();
        }

        if (!LicenseManager.validateLicense()) {
            showLicenseError();
            System.exit(1);
        }

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/splash.fxml"));
        Scene scene = new Scene(root);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean isDebugMode() {
        return System.getProperty("debug") != null;
    }

    private void showDebugInfo() {
        String hardwareId = HardwareUtil.getHardwareId();
        String message = "HARDWARE ID: " + hardwareId + "\n\n";

        TextInputDialog dialog = new TextInputDialog(hardwareId);
        dialog.setTitle("DEBUG MODE");
        dialog.setHeaderText("Información de Licencia");
        dialog.setContentText(message);
        dialog.showAndWait();
    }

    private void showLicenseError() {
        String hardwareId = HardwareUtil.getHardwareId();
        String errorMessage = "Hardware ID actual: " + hardwareId + "\n\n"
                + "Este software está vinculado a otro equipo.\n"
                + "Contacte al soporte técnico con este código: " + hardwareId;

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de Licencia");
        alert.setHeaderText("LICENCIA NO VÁLIDA");
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // Para modo debug: agregar -Ddebug=true en VM options
        launch(args);
    }
}