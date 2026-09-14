package com.channel.digital;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {

    public static void main(String[] args) throws IOException {
        startServer(8080);
    }

    public static HttpServer startServer(int port) throws IOException {
        HttpServer server = createServer(port);
        server.setExecutor(null);
        server.start();

        String env = System.getenv().getOrDefault("APP_ENV", "unknown");
        System.out.println("Agen46 Backend started on port " + port + " [ENV=" + env + "]");
        return server;
    }

    public static HttpServer createServer(int port) throws IOException {
        // ... ISI METHOD INI TETAP SAMA PERSIS SEPERTI SEBELUMNYA, TIDAK BERUBAH
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        String env = System.getenv("APP_ENV");
        if (env == null)
            env = "unknown";

        String color = resolveColor(env);

        final String finalEnv = env;
        final String finalColor = color;

        server.createContext("/", exchange -> {
            String html = "<html>"
                    + "<head><title>Agen46 Backend</title></head>"
                    + "<body style='background-color:" + finalColor
                    + "; color:white; font-family:sans-serif; text-align:center; padding-top:60px;'>"
                    + "<h1>Agen46 Backend</h1>"
                    + "<h2>Environment: " + finalEnv.toUpperCase() + "</h2>"
                    + "<p>Build: " + System.getenv().getOrDefault("BUILD_NUMBER", "N/A") + "</p>"
                    + "<img src='/photo' style='max-width:400px; border:4px solid white; border-radius:8px; margin-top:20px;' />"
                    + "</body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        });

        server.createContext("/photo", exchange -> {
            try (InputStream is = App.class.getResourceAsStream("/static/foto.jpg")) {
                if (is == null) {
                    String notFound = "Gambar tidak ditemukan";
                    exchange.sendResponseHeaders(404, notFound.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                    return;
                }
                byte[] imageBytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
                exchange.sendResponseHeaders(200, imageBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(imageBytes);
                }
            }
        });

        server.createContext("/api/v1/payments/health", exchange -> {
            String response = "{\"status\":\"UP\",\"environment\":\"" + finalEnv + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        return server;
    }

    static String resolveColor(String env) {
        switch (env) {
            case "development":
                return "#e74c3c";
            case "testing":
                return "#f1c40f";
            case "production":
                return "#2ecc71";
            case "production-rollback":
                return "#e67e22";
            case "production-rollback-auto":
                return "#3498db";
            default:
                return "#95a5a6";
        }
    }
}