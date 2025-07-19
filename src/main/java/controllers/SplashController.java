package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import util.AlertScheduler;
import util.DatabaseUtil;
import util.EstadoClienteService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SplashController {
    @FXML private StackPane rootPane;
    @FXML private ImageView imgLogo;
    @FXML private ProgressBar progressBar;

    @FXML
    public void initialize() {
        // Cargar imagen del logo
        imgLogo.setImage(new Image(getClass().getResourceAsStream("/images/mancar2.png")));

        // Configurar transición de entrada
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), rootPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Crear tarea para inicialización en segundo plano
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Paso 1: Actualizar progreso inicial (0-30%)
                for (int i = 0; i < 30; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(40);
                }

                // Paso 2: Inicializar base de datos
                DatabaseUtil.initDatabase();
                updateProgress(50, 100);

                // Paso 3: Iniciar servicios
                EstadoClienteService.iniciarActualizacionDiaria();
                updateProgress(70, 100);

                // Paso 4: Programar alertas
                ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleAtFixedRate(() -> new AlertScheduler().run(), 0, 1, TimeUnit.DAYS);
                updateProgress(90, 100);

                // Paso 5: Finalizar carga
                for (int i = 90; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(20);
                }

                return null;
            }
        };

        // Vincular progreso a la barra
        progressBar.progressProperty().bind(task.progressProperty());

        // Manejar eventos de la tarea
        task.setOnSucceeded(e -> abrirDashboard());
        task.setOnFailed(e -> {
            System.err.println("Error en splash screen: " + task.getException().getMessage());
            abrirDashboard(); // Intentar abrir dashboard de todas formas
        });

        // Iniciar tarea en segundo plano
        new Thread(task).start();
    }

    private void abrirDashboard() {
        Platform.runLater(() -> {
            try {
                // Cerrar splash
                Stage splashStage = (Stage) rootPane.getScene().getWindow();
                splashStage.close();

                // Abrir dashboard
                Stage stage = new Stage();
                Parent root = FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));
                Scene scene = new Scene(root, 900, 650);
                stage.setScene(scene);
                stage.setTitle("Panel de Control - Gimnasio");
                stage.setResizable(false);
                stage.show();
            } catch (Exception e) {
                System.err.println("Error abriendo dashboard: " + e.getMessage());
            }
        });
    }
}