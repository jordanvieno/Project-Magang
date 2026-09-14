package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

public class StatusServer {
    static AtomicReference<String> currentStage = new AtomicReference<>("idle");
    static AtomicReference<String> currentBuild = new AtomicReference<>("-");

    public static void main(String[] args) throws IOException {
        int port = 9000;
        HttpServer server = createServer(port);
        server.setExecutor(null);
        server.start();
        System.out.println("Status dashboard started on port " + port);
    }

    public static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/update", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String stage = "idle", build = "-";
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv[0].equals("stage"))
                        stage = kv[1];
                    if (kv[0].equals("build"))
                        build = kv[1];
                }
            }
            currentStage.set(stage);
            currentBuild.set(build);
            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.createContext("/", exchange -> {
            String stage = currentStage.get();
            String color = resolveColor(stage);
            String html = "<html><head><meta http-equiv='refresh' content='2'>"
                    + "<title>Pipeline Status</title></head>"
                    + "<body style='background-color:" + color
                    + "; color:white; font-family:sans-serif; text-align:center; padding-top:100px;'>"
                    + "<h1>Agen46 Deployment Status</h1>"
                    + "<h2>Tahap Terakhir: " + stage.toUpperCase() + "</h2>"
                    + "<p>Build: " + currentBuild.get() + "</p>"
                    + "</body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        });

        return server;
    }

    static String resolveColor(String stage) {
        switch (stage) {
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