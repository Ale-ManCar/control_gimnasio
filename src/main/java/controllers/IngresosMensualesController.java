package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import util.DatabaseUtil;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class IngresosMensualesController implements Initializable {

    @FXML
    private BarChart<String, Number> barChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        XYChart.Series<String, Number> datos = new XYChart.Series<>();

        String sql = "SELECT strftime('%m', fecha_pago) AS mes, SUM(monto) as total " +
                "FROM pagos WHERE strftime('%Y', fecha_pago) = strftime('%Y', 'now') " +
                "GROUP BY mes ORDER BY mes";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String mes = switch (rs.getString("mes")) {
                    case "01" -> "Ene";
                    case "02" -> "Feb";
                    case "03" -> "Mar";
                    case "04" -> "Abr";
                    case "05" -> "May";
                    case "06" -> "Jun";
                    case "07" -> "Jul";
                    case "08" -> "Ago";
                    case "09" -> "Sep";
                    case "10" -> "Oct";
                    case "11" -> "Nov";
                    case "12" -> "Dic";
                    default -> "";
                };
                datos.getData().add(new XYChart.Data<>(mes, rs.getDouble("total")));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        barChart.getData().add(datos);
    }

    @FXML
    private void handleVolver(javafx.event.ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("fxml/dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) barChart.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}