package com.channel.digital;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class App {

    static final long START_TIME = System.currentTimeMillis();
    static final AtomicLong healthRequestCount = new AtomicLong(0);
    static final AtomicLong testRequestCount = new AtomicLong(0);
    static final AtomicLong rootRequestCount = new AtomicLong(0);
    static final Map<String, Long> lastResponseTimeMs = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        startServer(port);
    }

    public static HttpServer startServer(int port) throws IOException {
        HttpServer server = createServer(port);
        server.setExecutor(null);
        server.start();

        String env = System.getenv().getOrDefault("APP_ENV", "unknown");
        System.out.println("Agen46 Backend started on port " + port + " [ENV=" + env + "]");
        return server;
    }

    static String getDbUrl() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "5432");
        String name = System.getenv().getOrDefault("DB_NAME", "agen46_dev");
        return "jdbc:postgresql://" + host + ":" + port + "/" + name;
    }

    static Connection getDbConnection() throws SQLException {
        String url = getDbUrl();
        String user = System.getenv().getOrDefault("DB_USER", "agen46");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "agen46pass");
        return DriverManager.getConnection(url, user, password);
    }

    static boolean isDatabaseHealthy() {
        try (Connection conn = getDbConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            System.out.println("DB health check gagal: " + e.getMessage());
            return false;
        }
    }

    public static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        String env = System.getenv("APP_ENV");
        if (env == null)
            env = "unknown";

        String color = resolveColor(env);

        final String finalEnv = env;
        final String finalColor = color;

        server.createContext("/", exchange -> {
            long start = System.currentTimeMillis();
            rootRequestCount.incrementAndGet();
            String html = "<html>"
                    + "<head><title>Agen46 Backend</title><meta http-equiv='refresh' content='5'></head>"
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
            lastResponseTimeMs.put("root", System.currentTimeMillis() - start);
        });

        server.createContext("/metrics", exchange -> {
            long uptimeSeconds = (System.currentTimeMillis() - START_TIME) / 1000;
            StringBuilder sb = new StringBuilder();
            sb.append("# HELP agen46_uptime_seconds Waktu aplikasi sudah berjalan\n");
            sb.append("# TYPE agen46_uptime_seconds counter\n");
            sb.append("agen46_uptime_seconds ").append(uptimeSeconds).append("\n");

            sb.append("# HELP agen46_requests_total Jumlah request per endpoint\n");
            sb.append("# TYPE agen46_requests_total counter\n");
            sb.append("agen46_requests_total{endpoint=\"root\"} ").append(rootRequestCount.get()).append("\n");
            sb.append("agen46_requests_total{endpoint=\"health\"} ").append(healthRequestCount.get()).append("\n");
            sb.append("agen46_requests_total{endpoint=\"test\"} ").append(testRequestCount.get()).append("\n");

            sb.append("# HELP agen46_last_response_time_ms Response time terakhir per endpoint (ms)\n");
            sb.append("# TYPE agen46_last_response_time_ms gauge\n");
            for (Map.Entry<String, Long> entry : lastResponseTimeMs.entrySet()) {
                sb.append("agen46_last_response_time_ms{endpoint=\"").append(entry.getKey())
                        .append("\"} ").append(entry.getValue()).append("\n");
            }

            sb.append("# HELP agen46_database_up Status koneksi database (1=up, 0=down)\n");
            sb.append("# TYPE agen46_database_up gauge\n");
            sb.append("agen46_database_up ").append(isDatabaseHealthy() ? 1 : 0).append("\n");

            String response = sb.toString();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
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
            long start = System.currentTimeMillis();
            healthRequestCount.incrementAndGet();
            boolean dbUp = isDatabaseHealthy();
            String status = dbUp ? "UP" : "DEGRADED";
            String response = "{\"status\":\"" + status + "\",\"environment\":\"" + finalEnv
                    + "\",\"database\":\"" + (dbUp ? "UP" : "DOWN") + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            lastResponseTimeMs.put("health", System.currentTimeMillis() - start);
        });

        server.createContext("/api/v1/payments/test", exchange -> {
            long start = System.currentTimeMillis();
            testRequestCount.incrementAndGet();
            String response;
            int statusCode = 200;
            try (Connection conn = getDbConnection()) {
                try (Statement createStmt = conn.createStatement()) {
                    createStmt.execute(
                            "CREATE TABLE IF NOT EXISTS payment_test (" +
                                    "id SERIAL PRIMARY KEY, " +
                                    "note TEXT, " +
                                    "created_at TIMESTAMP DEFAULT NOW())");
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO payment_test (note) VALUES (?)")) {
                    insertStmt.setString(1,
                            "test dari build " + System.getenv().getOrDefault("BUILD_NUMBER", "manual"));
                    insertStmt.executeUpdate();
                }

                StringBuilder rows = new StringBuilder();
                try (Statement selectStmt = conn.createStatement();
                        ResultSet rs = selectStmt.executeQuery(
                                "SELECT id, note, created_at FROM payment_test ORDER BY id DESC LIMIT 5")) {
                    while (rs.next()) {
                        if (rows.length() > 0)
                            rows.append(",");
                        rows.append("{\"id\":").append(rs.getInt("id"))
                                .append(",\"note\":\"").append(rs.getString("note"))
                                .append("\",\"created_at\":\"").append(rs.getTimestamp("created_at"))
                                .append("\"}");
                    }
                }
                response = "{\"result\":\"success\",\"rows\":[" + rows + "]}";
            } catch (SQLException e) {
                statusCode = 500;
                response = "{\"result\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}";
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            lastResponseTimeMs.put("test", System.currentTimeMillis() - start);
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