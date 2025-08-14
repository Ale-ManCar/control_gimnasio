import controllers.ReactivacionController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import util.DatabaseUtil;
import util.HardwareUtil;
import util.LicenseManager;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!LicenseManager.validateLicense()) {
            if (!showReactivationDialog(primaryStage)) {
                showLicenseError();
                System.exit(1);
            }
        }

        DatabaseUtil.initDatabase();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean showReactivationDialog(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reactivacion.fxml"));
            Parent root = loader.load();

            ReactivacionController controller = loader.getController();
            controller.setRequestCode(LicenseManager.generateReactivationRequest());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            return controller.wasSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
        launch(args);
    }
}