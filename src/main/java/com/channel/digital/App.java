package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class App {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/v1/payments/health", exchange -> {
            String response = "{\"status\":\"UP\"}";
            sendResponse(exchange, 200, response);
        });

        server.createContext("/", exchange -> {
            String response = "Agen46 Backend is running!";
            sendResponse(exchange, 200, response);
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Agen46 Backend started on port " + port);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}