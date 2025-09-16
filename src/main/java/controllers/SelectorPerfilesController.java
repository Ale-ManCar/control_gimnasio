package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.User;
import util.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SelectorPerfilesController {
    @FXML private FlowPane contenedorPerfiles;
    @FXML private Label lblMensaje;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        if (this.stage != null) {
            this.stage.setTitle("Seleccionar perfil");
            this.stage.setResizable(false);
        }
        Platform.runLater(this::cargarPerfiles);
    }

    @FXML
    private void initialize() {
        if (contenedorPerfiles != null) {
            contenedorPerfiles.setHgap(20);
            contenedorPerfiles.setVgap(20);
        }
        Platform.runLater(this::cargarPerfiles);
    }

    private void cargarPerfiles() {
        if (contenedorPerfiles == null) {
            return;
        }
        contenedorPerfiles.getChildren().clear();
        try {
            List<User> usuarios = UserService.listarUsuarios();
            if (usuarios.isEmpty()) {
                lblMensaje.setText("No hay usuarios registrados. Cree un administrador.");
                lblMensaje.setStyle("-fx-text-fill: #ff6b6b;");
            } else {
                lblMensaje.setText("Seleccione un perfil");
                lblMensaje.setStyle("-fx-text-fill: white;");
            }
            for (User user : usuarios) {
                contenedorPerfiles.getChildren().add(crearTarjetaUsuario(user));
            }
            contenedorPerfiles.getChildren().add(crearTarjetaAgregar());
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar los usuarios");
            lblMensaje.setStyle("-fx-text-fill: #ff6b6b;");
        }
    }

    private VBox crearTarjetaUsuario(User user) {
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/images/gym.png")));
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        Label nombre = new Label(user.getUsername());
        nombre.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        VBox tarjeta = new VBox(10, imageView, nombre);
        tarjeta.setPrefWidth(140);
        tarjeta.setStyle("-fx-alignment: center; -fx-padding: 15; -fx-background-color: rgba(0,0,0,0.45);"
                + " -fx-background-radius: 15; -fx-cursor: hand;");
        tarjeta.setOnMouseClicked(e -> abrirLogin(user));
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle("-fx-alignment: center; -fx-padding: 15;"
                + " -fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 15; -fx-cursor: hand;"));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle("-fx-alignment: center; -fx-padding: 15;"
                + " -fx-background-color: rgba(0,0,0,0.45); -fx-background-radius: 15; -fx-cursor: hand;"));
        return tarjeta;
    }

    private StackPane crearTarjetaAgregar() {
        Button boton = new Button("Añadir perfil");
        boton.setStyle("-fx-background-color: #ffb703; -fx-text-fill: #1f1f1f; -fx-font-weight: bold;"
                + " -fx-background-radius: 25; -fx-padding: 12 20; -fx-cursor: hand;");
        boton.setOnAction(e -> abrirRegistroRecepcionista());

        StackPane tarjeta = new StackPane(boton);
        tarjeta.setPrefSize(140, 140);
        tarjeta.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 15;"
                + " -fx-padding: 15; -fx-cursor: hand;");
        return tarjeta;
    }

    private void abrirLogin(User user) {
        if (stage == null) {
            mostrarAlerta("No se pudo determinar la ventana principal");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setStage(stage);
            controller.configurarParaUsuario(user);
            stage.setScene(new Scene(root, 400, 320));
            stage.setTitle("Iniciar sesión");
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir la pantalla de inicio de sesión: " + e.getMessage());
        }
    }

    private void abrirRegistroRecepcionista() {
        if (stage == null) {
            mostrarAlerta("No se pudo determinar la ventana principal");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_recepcionista.fxml"));
            Parent root = loader.load();
            RegistroRecepcionistaController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.setTitle("Nuevo recepcionista");
            dialog.setResizable(false);
            controller.setStage(dialog);
            controller.setOnSave(() -> Platform.runLater(this::cargarPerfiles));
            dialog.showAndWait();
        } catch (IOException e) {
            mostrarAlerta("No se pudo abrir el registro de recepcionista: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}