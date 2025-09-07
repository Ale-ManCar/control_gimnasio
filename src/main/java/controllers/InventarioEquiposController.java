package controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import models.Equipo;
import models.Proveedor;
import util.DatabaseUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InventarioEquiposController {

    @FXML private TableView<Equipo> tablaEquipos;
    @FXML private TableColumn<Equipo, String> colNombre;
    @FXML private TableColumn<Equipo, String> colMarca;
    @FXML private TableColumn<Equipo, Double> colPeso;
    @FXML private TableColumn<Equipo, Integer> colStock;
    @FXML private TableColumn<Equipo, Double> colPrecio;
    @FXML private TableColumn<Equipo, String> colProveedor;

    private ObservableList<Equipo> equipos = FXCollections.observableArrayList();
    private Map<Integer, String> proveedoresMap = new HashMap<>();

    @FXML
    public void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colPeso.setCellValueFactory(new PropertyValueFactory<>("peso"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        try {
            for (Proveedor p : DatabaseUtil.getProveedores()) {
                proveedoresMap.put(p.getId(), p.getNombre());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        colProveedor.setCellValueFactory(data -> {
            Integer id = data.getValue().getProveedorId();
            String nombre = id != null ? proveedoresMap.getOrDefault(id, "") : "";
            return new ReadOnlyStringWrapper(nombre);
        });
        cargarEquipos();
    }

    private void cargarEquipos() {
        try {
            equipos.setAll(DatabaseUtil.getEquipos());
            tablaEquipos.setItems(equipos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegistrarEquipo() {
        try {
            Dialog<Equipo> dialog = new Dialog<>();
            dialog.setTitle("Registrar Equipo");
            ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

            TextField txtNombre = new TextField();
            ComboBox<String> cbMarca = new ComboBox<>();
            cbMarca.setEditable(true);
            ComboBox<Double> cbPeso = new ComboBox<>();
            cbPeso.setEditable(true);
            TextField txtStock = new TextField();
            TextField txtPrecio = new TextField();
            ComboBox<Proveedor> cbProveedor = new ComboBox<>(DatabaseUtil.getProveedores());
            Button btnNuevoProveedor = new Button("Nuevo proveedor");

            btnNuevoProveedor.setOnAction(ev -> {
                Proveedor nuevo = mostrarFormularioProveedor();
                if (nuevo != null) {
                    try {
                        int nuevoId = DatabaseUtil.insertarProveedor(nuevo);
                        if (nuevoId > 0) {
                            nuevo.setId(nuevoId);
                            proveedoresMap.put(nuevoId, nuevo.getNombre());
                            cbProveedor.getItems().add(nuevo);
                            cbProveedor.getSelectionModel().select(nuevo);
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    cbMarca.getItems().setAll(DatabaseUtil.getMarcasPorNombre(newVal));
                    cbPeso.getItems().clear();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            cbMarca.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    cbPeso.getItems().clear();
                    return;
                }
                try {
                    cbPeso.getItems().setAll(DatabaseUtil.getPesosPorNombreMarca(txtNombre.getText(), newVal));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.add(new Label("Nombre:"),0,0); grid.add(txtNombre,1,0);
            grid.add(new Label("Marca:"),0,1); grid.add(cbMarca,1,1);
            grid.add(new Label("Peso:"),0,2); grid.add(cbPeso,1,2);
            grid.add(new Label("Stock:"),0,3); grid.add(txtStock,1,3);
            grid.add(new Label("Precio:"),0,4); grid.add(txtPrecio,1,4);
            grid.add(new Label("Proveedor:"),0,5); grid.add(cbProveedor,1,5); grid.add(btnNuevoProveedor,2,5);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(btn -> {
                if (btn == guardarBtn) {
                    try {
                        Integer provId = cbProveedor.getValue() != null ? cbProveedor.getValue().getId() : null;
                        String marca = cbMarca.getEditor().getText();
                        String pesoText = cbPeso.getEditor().getText();
                        double peso = pesoText.isEmpty() ? 0 : Double.parseDouble(pesoText);
                        return new Equipo(
                                txtNombre.getText(),
                                marca,
                                peso,
                                Integer.parseInt(txtStock.getText()),
                                Double.parseDouble(txtPrecio.getText()),
                                provId
                        );
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }
                return null;
            });

            Optional<Equipo> result = dialog.showAndWait();
            if (result.isPresent()) {
                Equipo equipo = result.get();
                int id = DatabaseUtil.insertarEquipo(equipo);
                if (id > 0) {
                    equipo.setId(id);
                    cargarEquipos();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleActualizarStock() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        TextInputDialog dialog = new TextInputDialog(String.valueOf(seleccionado.getStock()));
        dialog.setTitle("Actualizar Stock");
        dialog.setHeaderText("Nuevo stock para " + seleccionado.getNombre());
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(val -> {
            try {
                int nuevo = Integer.parseInt(val);
                DatabaseUtil.actualizarStockEquipo(seleccionado.getId(), nuevo);
                cargarEquipos();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleSubirPdf() {
        Equipo seleccionado = tablaEquipos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showOpenDialog(tablaEquipos.getScene().getWindow());
        if (file == null) return;
        TextInputDialog cantidadDialog = new TextInputDialog();
        cantidadDialog.setTitle("Cantidad comprada");
        cantidadDialog.setHeaderText("Cantidad adquirida");
        Optional<String> cantidadRes = cantidadDialog.showAndWait();
        if (!cantidadRes.isPresent()) return;
        TextInputDialog precioDialog = new TextInputDialog();
        precioDialog.setTitle("Precio unitario");
        precioDialog.setHeaderText("Precio por unidad");
        Optional<String> precioRes = precioDialog.showAndWait();
        if (!precioRes.isPresent()) return;
        try {
            int cantidad = Integer.parseInt(cantidadRes.get());
            double precio = Double.parseDouble(precioRes.get());
            Integer provId = seleccionado.getProveedorId();
            ObservableList<Proveedor> proveedores = DatabaseUtil.getProveedores();
            final Integer provIdBusqueda = provId;
            boolean proveedorValido = provIdBusqueda != null && provIdBusqueda > 0 &&
                    proveedores.stream().anyMatch(p -> p.getId() == provIdBusqueda);

            if (!proveedorValido) {
                Dialog<Proveedor> dialog = new Dialog<>();
                dialog.setTitle("Seleccionar Proveedor");
                ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

                ComboBox<Proveedor> cbProveedor = new ComboBox<>(proveedores);
                Button btnNuevoProveedor = new Button("Nuevo proveedor");

                btnNuevoProveedor.setOnAction(ev -> {
                    Proveedor nuevo = mostrarFormularioProveedor();
                    if (nuevo != null) {
                        try {
                            int nuevoId = DatabaseUtil.insertarProveedor(nuevo);
                            if (nuevoId > 0) {
                                nuevo.setId(nuevoId);
                                proveedoresMap.put(nuevoId, nuevo.getNombre());
                                cbProveedor.getItems().add(nuevo);
                                cbProveedor.getSelectionModel().select(nuevo);
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.add(new Label("Proveedor:"), 0, 0);
                grid.add(cbProveedor, 1, 0);
                grid.add(btnNuevoProveedor, 2, 0);
                dialog.getDialogPane().setContent(grid);

                dialog.setResultConverter(btn -> btn == guardarBtn ? cbProveedor.getValue() : null);

                Optional<Proveedor> result = dialog.showAndWait();
                if (!result.isPresent()) return;
                provId = result.get().getId();
            }

            DatabaseUtil.registrarCompra(provId, seleccionado, cantidad, precio, file.getAbsolutePath());
            cargarEquipos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Proveedor mostrarFormularioProveedor() {
        Dialog<Proveedor> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Proveedor");
        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        TextField txtContacto = new TextField();
        TextField txtTelefono = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Contacto:"), 0, 1);
        grid.add(txtContacto, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(txtTelefono, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == guardarBtn) {
                Proveedor p = new Proveedor();
                p.setNombre(txtNombre.getText());
                p.setContacto(txtContacto.getText());
                p.setTelefono(txtTelefono.getText());
                return p;
            }
            return null;
        });

        Optional<Proveedor> result = dialog.showAndWait();
        return result.orElse(null);
    }
}