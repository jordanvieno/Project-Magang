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
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.createContext("/", exchange -> {
            String stage = currentStage.get();
            String color;
            switch (stage) {
                case "development":
                    color = "#e74c3c";
                    break;
                case "testing":
                    color = "#f1c40f";
                    break;
                case "production":
                    color = "#2ecc71";
                    break;
                default:
                    color = "#95a5a6";
            }
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
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Status dashboard started on port " + port);
    }
}