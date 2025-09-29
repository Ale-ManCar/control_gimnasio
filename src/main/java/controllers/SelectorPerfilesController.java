package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.User;
import models.Role;
import util.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

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

            List<User> administradores = usuarios.stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .collect(Collectors.toList());
            List<User> recepcionistas = usuarios.stream()
                    .filter(u -> u.getRole() == Role.RECEPCIONISTA)
                    .collect(Collectors.toList());

            User administradorPrincipal = administradores.isEmpty() ? null : administradores.get(0);

            VBox disposicion = construirDisposicionPerfiles(administradorPrincipal, recepcionistas);
            contenedorPerfiles.getChildren().add(disposicion);
        } catch (SQLException e) {
            lblMensaje.setText("No se pudieron cargar los usuarios");
            lblMensaje.setStyle("-fx-text-fill: #ff6b6b;");
        }
    }

    private VBox construirDisposicionPerfiles(User administrador, List<User> recepcionistas) {
        VBox contenedor = new VBox(30);
        contenedor.setAlignment(Pos.TOP_CENTER);

        VBox tarjetaAdministrador = administrador != null ? crearTarjetaUsuario(administrador) : null;
        List<VBox> tarjetasRecepcionistas = recepcionistas.stream()
                .map(this::crearTarjetaUsuario)
                .collect(Collectors.toList());

        if (tarjetaAdministrador != null || !tarjetasRecepcionistas.isEmpty()) {
            GridPane grid = new GridPane();
            grid.setHgap(30);
            grid.setVgap(25);
            grid.setAlignment(Pos.CENTER);
            organizarPerfiles(grid, tarjetaAdministrador, tarjetasRecepcionistas);
            contenedor.getChildren().add(grid);
        }

        contenedor.getChildren().add(crearTarjetaAgregar());
        return contenedor;
    }

    private void organizarPerfiles(GridPane grid, VBox tarjetaAdministrador, List<VBox> tarjetasRecepcionistas) {
        int cantidadRecepcionistas = tarjetasRecepcionistas.size();

        if (cantidadRecepcionistas == 0) {
            prepararColumnas(grid, 1);
            if (tarjetaAdministrador != null) {
                grid.add(tarjetaAdministrador, 0, 0);
                GridPane.setHalignment(tarjetaAdministrador, HPos.CENTER);
                GridPane.setFillWidth(tarjetaAdministrador, false);
            }
            return;
        }

        switch (cantidadRecepcionistas) {
            case 1 -> colocarUnRecepcionista(grid, tarjetaAdministrador, tarjetasRecepcionistas.get(0));
            case 2 -> colocarDosRecepcionistas(grid, tarjetaAdministrador, tarjetasRecepcionistas);
            case 3 -> colocarTresRecepcionistas(grid, tarjetaAdministrador, tarjetasRecepcionistas);
            default -> colocarCuatroOMasRecepcionistas(grid, tarjetaAdministrador, tarjetasRecepcionistas);
        }
    }

    private void colocarUnRecepcionista(GridPane grid, VBox tarjetaAdministrador, VBox recepcionista) {
        prepararColumnas(grid, 1);
        int filaRecepcionista = 0;
        if (tarjetaAdministrador != null) {
            grid.add(tarjetaAdministrador, 0, 0);
            GridPane.setHalignment(tarjetaAdministrador, HPos.CENTER);
            GridPane.setFillWidth(tarjetaAdministrador, false);
            filaRecepcionista = 1;
        }
        grid.add(recepcionista, 0, filaRecepcionista);
        GridPane.setHalignment(recepcionista, HPos.CENTER);
        GridPane.setFillWidth(recepcionista, false);
    }

    private void colocarDosRecepcionistas(GridPane grid, VBox tarjetaAdministrador, List<VBox> tarjetasRecepcionistas) {
        prepararColumnas(grid, 2);
        int filaRecepcionistas = 0;
        if (tarjetaAdministrador != null) {
            grid.add(tarjetaAdministrador, 0, 0, 2, 1);
            GridPane.setHalignment(tarjetaAdministrador, HPos.CENTER);
            GridPane.setFillWidth(tarjetaAdministrador, false);
            filaRecepcionistas = 1;
        }
        grid.add(tarjetasRecepcionistas.get(0), 0, filaRecepcionistas);
        GridPane.setHalignment(tarjetasRecepcionistas.get(0), HPos.CENTER);
        GridPane.setFillWidth(tarjetasRecepcionistas.get(0), false);
        grid.add(tarjetasRecepcionistas.get(1), 1, filaRecepcionistas);
        GridPane.setHalignment(tarjetasRecepcionistas.get(1), HPos.CENTER);
        GridPane.setFillWidth(tarjetasRecepcionistas.get(1), false);
    }

    private void colocarTresRecepcionistas(GridPane grid, VBox tarjetaAdministrador, List<VBox> tarjetasRecepcionistas) {
        prepararColumnas(grid, 3);
        int filaRecepcionistas = 0;
        if (tarjetaAdministrador != null) {
            grid.add(tarjetaAdministrador, 1, 0);
            GridPane.setHalignment(tarjetaAdministrador, HPos.CENTER);
            GridPane.setFillWidth(tarjetaAdministrador, false);
            filaRecepcionistas = 1;
        }
        grid.add(tarjetasRecepcionistas.get(0), 0, filaRecepcionistas);
        GridPane.setHalignment(tarjetasRecepcionistas.get(0), HPos.CENTER);
        GridPane.setFillWidth(tarjetasRecepcionistas.get(0), false);
        grid.add(tarjetasRecepcionistas.get(1), 1, filaRecepcionistas);
        GridPane.setHalignment(tarjetasRecepcionistas.get(1), HPos.CENTER);
        GridPane.setFillWidth(tarjetasRecepcionistas.get(1), false);
        grid.add(tarjetasRecepcionistas.get(2), 2, filaRecepcionistas);
        GridPane.setHalignment(tarjetasRecepcionistas.get(2), HPos.CENTER);
        GridPane.setFillWidth(tarjetasRecepcionistas.get(2), false);
    }

    private void colocarCuatroOMasRecepcionistas(GridPane grid, VBox tarjetaAdministrador, List<VBox> tarjetasRecepcionistas) {
        prepararColumnas(grid, 2);
        int filaInicial = 0;
        if (tarjetaAdministrador != null) {
            grid.add(tarjetaAdministrador, 0, 0, 2, 1);
            GridPane.setHalignment(tarjetaAdministrador, HPos.CENTER);
            GridPane.setFillWidth(tarjetaAdministrador, false);
            filaInicial = 1;
        }

        for (int i = 0; i < tarjetasRecepcionistas.size(); i++) {
            int columna = i % 2;
            int fila = filaInicial + (i / 2);
            grid.add(tarjetasRecepcionistas.get(i), columna, fila);
            GridPane.setHalignment(tarjetasRecepcionistas.get(i), HPos.CENTER);
            GridPane.setFillWidth(tarjetasRecepcionistas.get(i), false);
        }
    }

    private void prepararColumnas(GridPane grid, int columnas) {
        grid.getColumnConstraints().clear();
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columnas);
            constraint.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(constraint);
        }
    }

    private VBox crearTarjetaUsuario(User user) {
        StackPane icono = crearIconoRol(user.getRole());
        Label nombre = new Label(user.getUsername());
        nombre.getStyleClass().add("nombre-usuario");

        VBox tarjeta = new VBox(12, icono, nombre);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setMinSize(140, 160);
        tarjeta.setPrefSize(140, 160);
        tarjeta.setMaxSize(140, 160);
        tarjeta.setFocusTraversable(true);
        tarjeta.getStyleClass().add("tarjeta-usuario");
        tarjeta.setOnMouseClicked(e -> abrirLogin(user));
        return tarjeta;
    }

    private StackPane crearIconoRol(Role role) {
        StackPane icono = new StackPane();
        icono.getStyleClass().add("icono-usuario");

        String estiloRol;
        String simbolo;
        if (role == Role.ADMIN) {
            estiloRol = "icono-admin";
            simbolo = "\uD83D\uDC51"; // 👑
        } else {
            estiloRol = "icono-recepcionista";
            simbolo = "\uD83D\uDCBC"; // 💼
        }
        icono.getStyleClass().add(estiloRol);

        Label etiqueta = new Label(simbolo);
        etiqueta.getStyleClass().add("emoji-icono");
        icono.getChildren().add(etiqueta);

        return icono;
    }

    private StackPane crearTarjetaAgregar() {
        Button boton = new Button("Añadir perfil");
        boton.getStyleClass().add("boton-agregar");
        boton.setOnAction(e -> abrirRegistroRecepcionista());

        StackPane tarjeta = new StackPane(boton);
        tarjeta.setMinSize(140, 160);
        tarjeta.setPrefSize(140, 160);
        tarjeta.setMaxSize(140, 160);
        tarjeta.getStyleClass().add("tarjeta-agregar");
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