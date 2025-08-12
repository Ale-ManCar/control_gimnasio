package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
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

    private final ObservableList<Coach> coaches = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarEstilosTabla();
        configurarFilas();

        tablaCoaches.setItems(coaches);
        cargarCoaches();
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
            private final Button btnPerfil = new Button("Perfil");
            {
                btnPerfil.setOnAction(e -> {
                    Coach coach = getTableView().getItems().get(getIndex());
                    verPerfil(coach);
                });
                btnPerfil.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5px;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPerfil);
            }
        });
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