package controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.Proveedor;
import models.Cotizacion;
import models.Producto;
import util.DatabaseUtil;
import util.AuditoriaUtil;
import util.SessionManager;
import java.io.IOException;

import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ProveedoresController implements Initializable {

    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colNombre;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colEmail;
    @FXML private TableColumn<Proveedor, String> colEstado;

    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private CheckBox chkActivo;

    @FXML private TableView<Cotizacion> tablaCotizaciones;
    @FXML private TableColumn<Cotizacion, String> colCotProducto;
    @FXML private TableColumn<Cotizacion, String> colCotPresentacion;
    @FXML private TableColumn<Cotizacion, Double> colCotPrecio;
    @FXML private TableColumn<Cotizacion, String> colCotVigencia;
    @FXML private TableColumn<Cotizacion, String> colCotCondiciones;

    @FXML private ComboBox<Producto> cbProducto;
    @FXML private TextField txtPresentacionCot;
    @FXML private TextField txtPrecioCot;
    @FXML private TextField txtVigenciaCot;
    @FXML private TextField txtCondicionesCot;

    private Proveedor proveedorSeleccionado;
    private Cotizacion cotizacionSeleccionada;

    private ObservableList<Producto> productos = FXCollections.observableArrayList();
    private Map<Integer, String> mapaProductos = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            productos = DatabaseUtil.getProductos();
            cbProducto.setItems(productos);
            productos.forEach(p -> mapaProductos.put(p.getId(), p.getNombre()));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEstado.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().isActivo() ? "Activo" : "Inactivo"));

        tablaProveedores.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            proveedorSeleccionado = n;
            if (n != null) {
                txtNombre.setText(n.getNombre());
                txtTelefono.setText(n.getTelefono());
                txtEmail.setText(n.getEmail());
                chkActivo.setSelected(n.isActivo());
                cargarCotizaciones(n.getId());
            } else {
                tablaCotizaciones.setItems(FXCollections.observableArrayList());
                nuevaCotizacion();
            }
        });

        colCotProducto.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(mapaProductos.getOrDefault(data.getValue().getProductoId(), "")));
        colCotPresentacion.setCellValueFactory(new PropertyValueFactory<>("presentacion"));
        colCotPrecio.setCellValueFactory(new PropertyValueFactory<>("precio"));
        colCotPrecio.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });
        colCotVigencia.setCellValueFactory(new PropertyValueFactory<>("vigencia"));
        colCotCondiciones.setCellValueFactory(new PropertyValueFactory<>("condiciones"));

        tablaCotizaciones.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            cotizacionSeleccionada = n;
            if (n != null) {
                cbProducto.getSelectionModel().select(productos.stream()
                        .filter(p -> p.getId() == n.getProductoId())
                        .findFirst().orElse(null));
                txtPresentacionCot.setText(n.getPresentacion());
                txtPrecioCot.setText(String.valueOf(n.getPrecio()));
                txtVigenciaCot.setText(n.getVigencia());
                txtCondicionesCot.setText(n.getCondiciones());
            }
        });

        cargarProveedores();
    }

    private void cargarProveedores() {
        try {
            tablaProveedores.setItems(DatabaseUtil.getProveedores(false));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void nuevoProveedor() {
        proveedorSeleccionado = null;
        txtNombre.clear();
        txtTelefono.clear();
        txtEmail.clear();
        chkActivo.setSelected(true);
        tablaProveedores.getSelectionModel().clearSelection();
        tablaCotizaciones.setItems(FXCollections.observableArrayList());
        nuevaCotizacion();
    }

    @FXML
    private void guardarProveedor() {
        String nombre = txtNombre.getText();
        if (nombre == null || nombre.isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Nombre requerido").showAndWait();
            return;
        }
        String telefono = txtTelefono.getText();
        String email = txtEmail.getText();
        boolean activo = chkActivo.isSelected();
        try {
            if (proveedorSeleccionado == null) {
                Integer id = DatabaseUtil.insertarProveedor(nombre, telefono, email);
                if (id != null) {
                    AuditoriaUtil.registrar(
                            SessionManager.getUsuarioActual().getNombre(),
                            "ALTA_PROVEEDOR",
                            "PROVEEDOR",
                            id,
                            nombre);
                    if (!activo) {
                        DatabaseUtil.cambiarEstadoProveedor(id, false);
                        AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), "STATUS_CHANGE", "PROVEEDOR", id, "INACTIVO");
                    }
                }
            } else {
                int id = proveedorSeleccionado.getId();
                boolean estadoPrevio = proveedorSeleccionado.isActivo();
                DatabaseUtil.actualizarProveedor(id, nombre, telefono, email);
                AuditoriaUtil.registrar(
                        SessionManager.getUsuarioActual().getNombre(),
                        "EDICION_PROVEEDOR",
                        "PROVEEDOR",
                        id,
                        nombre);
                if (estadoPrevio != activo) {
                    DatabaseUtil.cambiarEstadoProveedor(id, activo);
                    AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(), "STATUS_CHANGE", "PROVEEDOR", id, activo ? "ACTIVO" : "INACTIVO");
                }
            }
            cargarProveedores();
            nuevoProveedor();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error guardando proveedor").showAndWait();
        }
    }

    private void cargarCotizaciones(int proveedorId) {
        ObservableList<Cotizacion> lista = FXCollections.observableArrayList();
        String sql = "SELECT id, proveedor_id, producto_id, presentacion, precio, vigencia, condiciones FROM cotizaciones WHERE proveedor_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proveedorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Cotizacion c = new Cotizacion();
                c.setId(rs.getInt("id"));
                c.setProveedorId(rs.getInt("proveedor_id"));
                c.setProductoId(rs.getInt("producto_id"));
                c.setPresentacion(rs.getString("presentacion"));
                c.setPrecio(rs.getDouble("precio"));
                c.setVigencia(rs.getString("vigencia"));
                c.setCondiciones(rs.getString("condiciones"));
                lista.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        tablaCotizaciones.setItems(lista);
        nuevaCotizacion();
    }

    @FXML
    private void nuevaCotizacion() {
        cotizacionSeleccionada = null;
        cbProducto.getSelectionModel().clearSelection();
        txtPresentacionCot.clear();
        txtPrecioCot.clear();
        txtVigenciaCot.clear();
        txtCondicionesCot.clear();
        tablaCotizaciones.getSelectionModel().clearSelection();
    }

    @FXML
    private void guardarCotizacion() {
        if (proveedorSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un proveedor").showAndWait();
            return;
        }
        Producto prod = cbProducto.getValue();
        if (prod == null) {
            new Alert(Alert.AlertType.ERROR, "Seleccione un producto").showAndWait();
            return;
        }
        String presentacion = txtPresentacionCot.getText();
        double precio;
        try {
            precio = Double.parseDouble(txtPrecioCot.getText());
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Precio inválido").showAndWait();
            return;
        }
        String vigencia = txtVigenciaCot.getText();
        String condiciones = txtCondicionesCot.getText();

        try {
            if (cotizacionSeleccionada == null) {
                Cotizacion c = new Cotizacion(proveedorSeleccionado.getId(), prod.getId(),
                        presentacion, precio, vigencia, condiciones);
                int id = DatabaseUtil.insertarCotizacion(c);
                AuditoriaUtil.registrar(
                        SessionManager.getUsuarioActual().getNombre(),
                        "ALTA_COTIZACION",
                        "COTIZACION",
                        id,
                        "Proveedor: " + proveedorSeleccionado.getNombre());
            } else {
                cotizacionSeleccionada.setProductoId(prod.getId());
                cotizacionSeleccionada.setPresentacion(presentacion);
                cotizacionSeleccionada.setPrecio(precio);
                cotizacionSeleccionada.setVigencia(vigencia);
                cotizacionSeleccionada.setCondiciones(condiciones);
                DatabaseUtil.actualizarCotizacion(cotizacionSeleccionada);
                AuditoriaUtil.registrar(
                        SessionManager.getUsuarioActual().getNombre(),
                        "EDICION_COTIZACION",
                        "COTIZACION",
                        cotizacionSeleccionada.getId(),
                        "Proveedor: " + proveedorSeleccionado.getNombre());
            }
            cargarCotizaciones(proveedorSeleccionado.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error guardando cotización").showAndWait();
        }
    }
    @FXML
    private void abrirComparador() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/comparador.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Comparador de Precios");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}