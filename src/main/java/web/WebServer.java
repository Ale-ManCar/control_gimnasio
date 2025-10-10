package web;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;
import util.DatabaseUtil;
import web.repository.ClienteRepository;

import java.util.Map;

/**
 * Punto de entrada minimalista para exponer el backend actual vía HTTP.
 */
public final class WebServer {
    private WebServer() {
    }

    public static void main(String[] args) {
        DatabaseUtil.initDatabase();
        configurarPuerto();
        configurarArchivosEstaticos();
        configurarCors();
        registrarRutas();
    }

    private static void configurarPuerto() {
        String port = System.getenv().getOrDefault("PORT", "8080");
        Spark.port(Integer.parseInt(port));
    }

    private static void configurarArchivosEstaticos() {
        Spark.staticFiles.location("/public");
    }

    private static void configurarCors() {
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        Filter corsFilter = (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        };

        Spark.before(corsFilter);
        Spark.afterAfter(corsFilter);
    }

    private static void registrarRutas() {
        ClienteRepository clienteRepository = new ClienteRepository();

        Spark.get("/api/status", WebServer::status, WebServer::json);
        Spark.get("/api/clientes", (request, response) -> clienteRepository.findAll(), WebServer::json);

        Spark.notFound((req, res) -> {
            res.type("application/json");
            return JsonUtil.toJson(Map.of(
                    "path", req.pathInfo(),
                    "message", "Recurso no encontrado"
            ));
        });

        Spark.internalServerError((req, res) -> {
            res.type("application/json");
            return JsonUtil.toJson(Map.of(
                    "path", req.pathInfo(),
                    "message", "Error inesperado"
            ));
        });
    }

    private static Object status(Request request, Response response) {
        response.type("application/json");
        return Map.of("status", "ok");
    }

    private static String json(Object model) {
        return JsonUtil.toJson(model);
    }
}
