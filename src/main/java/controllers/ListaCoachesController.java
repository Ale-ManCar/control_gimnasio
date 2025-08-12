package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import models.Coach;
import util.DatabaseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ListaCoachesController {

    @FXML private TableView<Coach> tablaCoaches;
    @FXML private TableColumn<Coach, String> colNombre;
    @FXML private TableColumn<Coach, String> colArea;
    @FXML private TableColumn<Coach, Void> colAcciones;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;

    private final ObservableList<Coach> coaches = FXCollections.observableArrayList();
    private final ObservableList<Coach> coachesOriginales = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarEstilosTabla();
        configurarFilas();
        tablaCoaches.setItems(coaches);
        cargarCoaches();
        coachesOriginales.setAll(coaches);
        configurarBusqueda();
        ajustarColumnas();
        ocultarScrollBars();
    }

    private void configurarColumnas() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("area"));

        colNombre.setCellFactory(column -> new TableCell<Coach, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: black; " +
                            "-fx-background-color: transparent;");
                }
            }
        });

        colArea.setCellFactory(column -> new TableCell<Coach, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: black; " +
                            "-fx-background-color: transparent;");
                }
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnPerfil = new Button();
            private final Button btnEditar = new Button();
            private final Button btnEliminar = new Button();
            private final HBox contenedor = new HBox(10);
            {
                FontIcon iconPerfil = new FontIcon(FontAwesomeSolid.USER);
                iconPerfil.setIconColor(Color.web("#007bff"));
                btnPerfil.setGraphic(iconPerfil);
                btnPerfil.setOnAction(e -> {
                    Coach coach = getTableView().getItems().get(getIndex());
                    verPerfil(coach);
                });

                FontIcon iconEditar = new FontIcon(FontAwesomeSolid.EDIT);
                iconEditar.setIconColor(Color.web("#28a745"));
                btnEditar.setGraphic(iconEditar);
                btnEditar.setOnAction(e -> {
                    Coach coach = getTableView().getItems().get(getIndex());
                    editarCoach(coach);
                });

                FontIcon iconEliminar = new FontIcon(FontAwesomeSolid.TRASH);
                iconEliminar.setIconColor(Color.web("#dc3545"));
                btnEliminar.setGraphic(iconEliminar);
                btnEliminar.setOnAction(e -> {
                    Coach coach = getTableView().getItems().get(getIndex());
                    eliminarCoach(coach);
                });

                String estilo = "-fx-background-color: transparent; -fx-cursor: hand;";
                btnPerfil.setStyle(estilo);
                btnEditar.setStyle(estilo);
                btnEliminar.setStyle(estilo);

                contenedor.getChildren().addAll(btnPerfil, btnEditar, btnEliminar);
                contenedor.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        });
    }

    private void ajustarColumnas() {
        tablaCoaches.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNombre.prefWidthProperty().bind(tablaCoaches.widthProperty().multiply(0.4));
        colArea.prefWidthProperty().bind(tablaCoaches.widthProperty().multiply(0.3));
        colAcciones.prefWidthProperty().bind(tablaCoaches.widthProperty().multiply(0.2));
    }

    private void ocultarScrollBars() {
        tablaCoaches.skinProperty().addListener((obs, oldSkin, newSkin) ->
                tablaCoaches.lookupAll(".scroll-bar").forEach(node -> {
                    node.setVisible(false);
                    node.setManaged(false);
                })
        );
    }

    private void configurarEstilosTabla() {
        tablaCoaches.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-background-color: #ffffff;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );
    }

    private void configurarFilas() {
        tablaCoaches.setRowFactory(tv -> {
            TableRow<Coach> row = new TableRow<>() {
                @Override
                protected void updateItem(Coach coach, boolean empty) {
                    super.updateItem(coach, empty);
                    if (empty || coach == null) {
                        setStyle("");
                        setTooltip(null);
                    } else {
                        if (isSelected()) {
                            setStyle("-fx-background-color: #e6f2ff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else if (isHover()) {
                            setStyle("-fx-background-color: #e6f2ff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        } else {
                            setStyle("-fx-background-color: #ffffff; " +
                                    "-fx-border-color: #e0e0e0; " +
                                    "-fx-border-width: 0 0 1px 0;");
                        }
                    }
                }
            };

            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    row.setStyle("-fx-background-color: #e6f2ff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else {
                    if (row.isHover()) {
                        row.setStyle("-fx-background-color: #e6f2ff; " +
                                "-fx-border-color: #e0e0e0; " +
                                "-fx-border-width: 0 0 1px 0;");
                    } else {
                        row.setStyle("-fx-background-color: #ffffff; " +
                                "-fx-border-color: #e0e0e0; " +
                                "-fx-border-width: 0 0 1px 0;");
                    }
                }
            });

            row.hoverProperty().addListener((obs, oldVal, isHovering) -> {
                if (isHovering && !row.isSelected()) {
                    row.setStyle("-fx-background-color: #e6f2ff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                } else if (!row.isSelected()) {
                    row.setStyle("-fx-background-color: #ffffff; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-width: 0 0 1px 0;");
                }
            });

            return row;
        });
    }

    private void cargarCoaches() {
        coaches.clear();
        String sql = "SELECT id, nombres, apellidos, area, telefono, foto_path FROM coaches";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                coaches.add(new Coach(
                        rs.getInt("id"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("area"),
                        rs.getString("telefono"),
                        rs.getString("foto_path")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void configurarBusqueda() {
        txtBuscar.setPromptText("Buscar coach...");
        txtBuscar.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-border-color: #ced4da;" +
                        "-fx-background-color: #ffffff;"
        );

        btnLimpiar.setStyle(
                "-fx-background-color: #6C757D;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8px 15px;" +
                        "-fx-background-radius: 20px;" +
                        "-fx-border-radius: 20px;" +
                        "-fx-cursor: hand;"
        );
        btnLimpiar.setOnMouseEntered(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #5a6268;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );
        btnLimpiar.setOnMouseExited(e ->
                btnLimpiar.setStyle(
                        "-fx-background-color: #6C757D;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 8px 15px;" +
                                "-fx-background-radius: 20px;" +
                                "-fx-border-radius: 20px;" +
                                "-fx-cursor: hand;"
                )
        );

        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            filtrarCoaches();
        });
    }

    private void filtrarCoaches() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            tablaCoaches.setItems(coachesOriginales);
        } else {
            ObservableList<Coach> filtrados = FXCollections.observableArrayList();
            for (Coach coach : coachesOriginales) {
                if (coach.getNombreCompleto().toLowerCase().contains(filtro) ||
                        coach.getArea().toLowerCase().contains(filtro)) {
                    filtrados.add(coach);
                }
            }
            tablaCoaches.setItems(filtrados);
        }

        tablaCoaches.refresh();
    }

    @FXML
    private void limpiarFiltro() {
        Coach seleccionado = tablaCoaches.getSelectionModel().getSelectedItem();
        int scrollPosition = tablaCoaches.getSelectionModel().getSelectedIndex();

        txtBuscar.clear();
        tablaCoaches.setItems(coachesOriginales);

        if (seleccionado != null) {
            tablaCoaches.getSelectionModel().select(seleccionado);
        }
        tablaCoaches.scrollTo(scrollPosition);

        tablaCoaches.refresh();
    }

    private void verPerfil(Coach coach) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/perfil_coach.fxml"));
            Parent root = loader.load();
            PerfilCoachController controller = loader.getController();
            controller.setCoach(coach);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Perfil del Coach");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void editarCoach(Coach coach) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro_coach.fxml"));
            Parent root = loader.load();
            RegistroCoachController controller = loader.getController();
            controller.setCoach(coach);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Editar Coach");
            stage.showAndWait();
            cargarCoaches();
            coachesOriginales.setAll(coaches);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void eliminarCoach(Coach coach) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea eliminar este coach?", ButtonType.OK, ButtonType.CANCEL);
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        String sql = "DELETE FROM coaches WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, coach.getId());
            stmt.executeUpdate();
            recargarCoaches();
            mostrarToastExito("Coach eliminado correctamente");;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void recargarCoaches() {
        tablaCoaches.getItems().clear();
        cargarCoaches();
        coachesOriginales.setAll(coaches);
        filtrarCoaches();
    }

    private void mostrarToastExito(String mensaje) {
        try {
            Stage stage = (Stage) tablaCoaches.getScene().getWindow();
            ToastController.showToast(stage, mensaje, ToastController.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void volverRegistro(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/registro_coach.fxml"));
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Registro de Coach");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}