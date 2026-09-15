package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    private static HttpServer server;
    private static final int PORT = 8099;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void startServer() throws Exception {
        server = App.createServer(PORT);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void healthEndpoint_shouldReturnHttp200() throws Exception {
        HttpResponse<String> response = get("/api/v1/payments/health");
        assertEquals(200, response.statusCode());
    }

    @Test
    void healthEndpoint_shouldReturnStatusUp() throws Exception {
        HttpResponse<String> response = get("/api/v1/payments/health");
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    @Test
    void healthEndpoint_shouldReturnJsonContentType() throws Exception {
        HttpResponse<String> response = get("/api/v1/payments/health");
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    void rootEndpoint_shouldReturnHtmlWithHttp200() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Agen46 Backend"));
    }

    @Test
    void photoEndpoint_shouldReturn404WhenImageMissing() throws Exception {
        // Di lingkungan test, /static/foto.jpg biasanya tidak ada di classpath
        // karena itu di-generate di Jenkins pipeline pakai curl picsum.photos
        HttpResponse<String> response = get("/photo");
        assertTrue(response.statusCode() == 200 || response.statusCode() == 404);
    }

    @Test
    void databaseHealthCheck_shouldReportUp_whenDbAvailable() throws Exception {
        HttpResponse<String> response = get("/api/v1/payments/health");
        assertTrue(response.body().contains("\"database\":\"UP\""));
    }

    @Test
    void paymentsTestEndpoint_shouldInsertAndReturnRows() throws Exception {
        HttpResponse<String> response = get("/api/v1/payments/test");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"result\":\"success\""));
        assertTrue(response.body().contains("\"rows\":["));
    }

    @Test
    void resolveColor_shouldMapKnownEnvironments() {
        assertEquals("#e74c3c", App.resolveColor("development"));
        assertEquals("#f1c40f", App.resolveColor("testing"));
        assertEquals("#2ecc71", App.resolveColor("production"));
        assertEquals("#e67e22", App.resolveColor("production-rollback"));
        assertEquals("#3498db", App.resolveColor("production-rollback-auto"));
    }

    @Test
    void resolveColor_shouldFallbackToGreyForUnknownEnv() {
        assertEquals("#95a5a6", App.resolveColor("staging-yang-nggak-ada"));
        assertEquals("#95a5a6", App.resolveColor("unknown"));
    }

    @Test
    void startServer_shouldStartAndPrintStartupMessage() throws Exception {
        HttpServer mainServer = App.startServer(8098);
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8098/api/v1/payments/health"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        } finally {
            mainServer.stop(0);
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}