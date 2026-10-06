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
                    String[] kv = param.split("=", 2);
                    if (kv.length < 2) continue;
                    if (kv[0].equals("stage") && kv[1].matches("^[a-zA-Z0-9-]+$"))
                        stage = kv[1];
                    if (kv[0].equals("build") && kv[1].matches("^\\d+$"))
                        build = kv[1];
                }
            }
            currentStage.set(stage);
            currentBuild.set(build);
            String response = "OK";
            App.sendResponse(exchange, 200, response, "text/plain");
        });

        server.createContext("/", exchange -> {
            String stage = currentStage.get();
            String color = App.resolveColor(stage);
            String html = "<html><head><meta http-equiv='refresh' content='2'>"
                    + "<title>Pipeline Status</title></head>"
                    + "<body style='background-color:" + color
                    + "; color:white; font-family:sans-serif; text-align:center; padding-top:100px;'>"
                    + "<h1>Agen46 Deployment Status</h1>"
                    + "<h2>Tahap Terakhir: " + stage.toUpperCase() + "</h2>"
                    + "<p>Build: " + currentBuild.get() + "</p>"
                    + "</body></html>";
            App.sendResponse(exchange, 200, html, "text/html");
        });

        return server;
    }


}