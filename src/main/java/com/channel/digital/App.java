package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App
{
    public static void main( String[] args ) throws IOException
    {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        String env = System.getenv("APP_ENV");
        if (env == null) env = "unknown";

        String color;
        switch (env) {
            case "production":
                color = "#2ecc71";
                break;
            case "testing":
                color = "#f1c40f";
                break;
            case "development":
                color = "#e74c3c";
                break;
            default:
                color = "#95a5a6";
        }

        final String finalEnv = env;
        final String finalColor = color;

        server.createContext("/", exchange -> {
            String html = "<html>"
                + "<head><title>Agen46 Backend</title></head>"
                + "<body style='background-color:" + finalColor + "; color:white; font-family:sans-serif; text-align:center; padding-top:100px;'>"
                + "<h1>Agen46 Backend</h1>"
                + "<h2>Environment: " + finalEnv.toUpperCase() + "</h2>"
                + "<p>Build: " + System.getenv().getOrDefault("BUILD_NUMBER", "N/A") + "</p>"
                + "</body></html>";
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
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
