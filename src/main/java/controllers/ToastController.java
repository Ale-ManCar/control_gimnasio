package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label; // Changed from Text
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastController {

    public static final int SUCCESS = 1;
    public static final int ERROR = 2;
    public static final int INFO = 3;

    @FXML private StackPane toastContainer;
    @FXML private ImageView imgIcon;
    @FXML private Label lblMensaje; // Changed from Text to Label

    public static void showToast(Stage ownerStage, String message, int type) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ToastController.class.getResource("/fxml/toast.fxml")
            );

            // Create controller instance before loading
            ToastController controller = new ToastController();
            loader.setController(controller);

            StackPane toast = loader.load();

            // Configurar según tipo
            switch (type) {
                case SUCCESS:
                    controller.imgIcon.setImage(new Image("/images/gym.png"));
                    controller.toastContainer.setStyle("-fx-background-color: rgba(40,167,69,0.9);");
                    break;
                case ERROR:
                    controller.imgIcon.setImage(new Image("/images/gym.png"));
                    controller.toastContainer.setStyle("-fx-background-color: rgba(220,53,69,0.9);");
                    break;
                case INFO:
                    controller.imgIcon.setImage(new Image("/images/gym.png"));
                    controller.toastContainer.setStyle("-fx-background-color: rgba(23,162,184,0.9);");
                    break;
            }

            controller.lblMensaje.setText(message);

            Stage toastStage = new Stage();
            toastStage.initOwner(ownerStage);
            toastStage.initStyle(StageStyle.TRANSPARENT);
            toastStage.setScene(new Scene(toast));

            // Posicionar en la esquina inferior derecha
            toastStage.setX(ownerStage.getX() + ownerStage.getWidth() - 320);
            toastStage.setY(ownerStage.getY() + ownerStage.getHeight() - 100);

            // Animación de entrada
            toastStage.show();
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toast);
            slideIn.setToY(0);
            slideIn.setFromY(100);
            slideIn.play();

            // Desvanecer después de 3 segundos
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(500), toast);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> toastStage.close());
                fadeOut.play();
            });
            delay.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}