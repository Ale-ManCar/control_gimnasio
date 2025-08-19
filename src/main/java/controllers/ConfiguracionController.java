package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import models.Config;
import util.AuditoriaUtil;
import util.DatabaseUtil;
import util.SessionManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ConfiguracionController implements Initializable {

    @FXML private TextField txtPlanBasico;
    @FXML private TextField txtPlanPremium;
    @FXML private TextField txtUmbralStock;
    @FXML private TextArea txtPlantillaBienvenida;
    @FXML private TextField txtRutaReportes;
    @FXML private TextField txtRutaAdjuntos;
    @FXML private TextArea txtMensajeWhatsapp;
    @FXML private TextArea txtMensajeRegistro;
    @FXML private TextArea txtMensajeRenovacion;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Config cfg = DatabaseUtil.getConfiguracion();
        if (cfg != null) {
            txtPlanBasico.setText(String.valueOf(cfg.getPlanBasico()));
            txtPlanPremium.setText(String.valueOf(cfg.getPlanPremium()));
            txtUmbralStock.setText(String.valueOf(cfg.getUmbralStock()));
            txtPlantillaBienvenida.setText(cfg.getPlantillaBienvenida());
            txtRutaReportes.setText(cfg.getRutaReportes());
            txtRutaAdjuntos.setText(cfg.getRutaAdjuntos());
            txtMensajeWhatsapp.setText(cfg.getMensajeWhatsapp());
            txtMensajeRegistro.setText(cfg.getMensajeRegistro());
            txtMensajeRenovacion.setText(cfg.getMensajeRenovacion());
        }
    }

    @FXML
    private void seleccionarRutaReportes() {
        txtRutaReportes.setText(seleccionarDirectorio());
    }

    @FXML
    private void seleccionarRutaAdjuntos() {
        txtRutaAdjuntos.setText(seleccionarDirectorio());
    }

    private String seleccionarDirectorio() {
        DirectoryChooser chooser = new DirectoryChooser();
        Stage stage = (Stage) txtPlanBasico.getScene().getWindow();
        File dir = chooser.showDialog(stage);
        return dir != null ? dir.getAbsolutePath() : null;
    }

    @FXML
    private void guardarConfiguracion() {
        if (!SessionManager.isAdmin()) {
            return;
        }
        try {
            Config cfg = new Config();
            cfg.setPlanBasico(parseDouble(txtPlanBasico.getText()));
            cfg.setPlanPremium(parseDouble(txtPlanPremium.getText()));
            cfg.setUmbralStock(parseInt(txtUmbralStock.getText()));
            cfg.setPlantillaBienvenida(txtPlantillaBienvenida.getText());
            cfg.setRutaReportes(txtRutaReportes.getText());
            cfg.setRutaAdjuntos(txtRutaAdjuntos.getText());
            cfg.setMensajeWhatsapp(txtMensajeWhatsapp.getText());
            cfg.setMensajeRegistro(txtMensajeRegistro.getText());
            cfg.setMensajeRenovacion(txtMensajeRenovacion.getText());
            DatabaseUtil.actualizarConfiguracion(cfg);
            AuditoriaUtil.registrar(SessionManager.getUsuarioActual().getNombre(),
                    "CONF_ACTUALIZADA", "CONFIG", null, "Cambios en configuración");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double parseDouble(String text) {
        try { return Double.parseDouble(text); } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String text) {
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return 0; }
    }
}