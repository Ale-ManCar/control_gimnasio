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
import javafx.util.Duration;
import util.AlertScheduler;
import util.AuditoriaScheduler;
import util.BackupUtil;
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
        imgLogo.setImage(new Image("images/mancar2.png"));

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

                // Paso 4: Programar tareas en segundo plano
                AlertScheduler.iniciar();
                AuditoriaScheduler.iniciar();
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(new BackupUtil(), 0, 1, TimeUnit.DAYS);
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
        task.setOnSucceeded(e -> abrirEntrada());
        task.setOnFailed(e -> {
            System.err.println("Error en splash screen: " + task.getException().getMessage());
            abrirEntrada(); // Intentar abrir flujo de autenticación de todas formas
        });

        // Iniciar tarea en segundo plano
        new Thread(task).start();
    }

    private void abrirEntrada() {
        Platform.runLater(() -> {
            try {
                // Cerrar splash
                Stage splashStage = (Stage) rootPane.getScene().getWindow();
                splashStage.close();

                // Abrir login
                int totalUsuarios = DatabaseUtil.getTotalUsuarios();
                String vista = totalUsuarios == 0 ? "/fxml/crear_admin.fxml" : "/fxml/selector_perfiles.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(vista));
                Parent root = loader.load();
                Stage stage = new Stage();
                if (totalUsuarios == 0) {
                    CrearAdminController controller = loader.getController();
                    controller.setStage(stage);
                    stage.setScene(new Scene(root, 420, 320));
                    stage.setTitle("Crear administrador");
                } else {
                    SelectorPerfilesController controller = loader.getController();
                    controller.setStage(stage);
                    stage.setScene(new Scene(root, 700, 420));
                    stage.setTitle("Seleccionar perfil");
                }
                stage.setResizable(false);
                stage.show();
            } catch (Exception e) {
                System.err.println("Error abriendo login: " + e.getMessage());
            }
        });
    }
}