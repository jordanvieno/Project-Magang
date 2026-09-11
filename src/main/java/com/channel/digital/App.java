package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        String env = System.getenv("APP_ENV");
        if (env == null)
            env = "unknown";

        String color;
        switch (env) {
            case "development":
                color = "#e74c3c";
                break;
            case "testing":
                color = "#f1c40f";
                break;
            case "production":
                color = "#2ecc71";
                break;
            case "production-rollback":
                color = "#e67e22";
                break;
            case "production-rollback-auto":
                color = "#3498db";
                break;
            default:
                color = "#95a5a6";
        }

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
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        });

        server.createContext("/photo", exchange -> {
            InputStream is = App.class.getResourceAsStream("/static/foto.jpg");
            if (is == null) {
                String notFound = "Gambar tidak ditemukan";
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(notFound.getBytes());
                os.close();
                return;
            }
            byte[] imageBytes = is.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, imageBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(imageBytes);
            os.close();
            is.close();
        });

        server.createContext("/api/v1/payments/health", exchange -> {
            String response = "{\"status\":\"UP\",\"environment\":\"" + finalEnv + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Agen46 Backend started on port " + port + " [ENV=" + finalEnv + "]");
    }
}
