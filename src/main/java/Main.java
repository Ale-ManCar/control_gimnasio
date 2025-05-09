import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carga el diseño FXML de la pantalla de registro
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/registro_cliente.fxml"));

        // Configura la ventana principal
        primaryStage.setTitle("Sistema de Gimnasio - Registro");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setResizable(false); // Evita redimensionar
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args); // Inicia la aplicación
    }
}