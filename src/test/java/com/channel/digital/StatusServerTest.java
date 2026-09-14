package com.channel.digital;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusServerTest {

    private static final int PORT = 8097;
    private static final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = StatusServer.createServer(PORT);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void updateEndpoint_shouldReturnOkAndStoreStageAndBuild() throws Exception {
        HttpResponse<String> response = get("/update?stage=testing&build=42");
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
        assertEquals("testing", StatusServer.currentStage.get());
        assertEquals("42", StatusServer.currentBuild.get());
    }

    @Test
    void updateEndpoint_withNoQuery_shouldDefaultToIdle() throws Exception {
        HttpResponse<String> response = get("/update");
        assertEquals(200, response.statusCode());
        assertEquals("idle", StatusServer.currentStage.get());
        assertEquals("-", StatusServer.currentBuild.get());
    }

    @Test
    void rootEndpoint_shouldReflectCurrentStageAndBuild() throws Exception {
        get("/update?stage=production&build=7");
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("PRODUCTION"));
        assertTrue(response.body().contains("Build: 7"));
    }

    @Test
    void rootEndpoint_shouldReturnHtmlContentType() throws Exception {
        HttpResponse<String> response = get("/");
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    }

    @Test
    void resolveColor_shouldMapAllKnownStages() {
        assertEquals("#e74c3c", StatusServer.resolveColor("development"));
        assertEquals("#f1c40f", StatusServer.resolveColor("testing"));
        assertEquals("#2ecc71", StatusServer.resolveColor("production"));
        assertEquals("#e67e22", StatusServer.resolveColor("production-rollback"));
        assertEquals("#3498db", StatusServer.resolveColor("production-rollback-auto"));
        assertEquals("#95a5a6", StatusServer.resolveColor("idle"));
        assertEquals("#95a5a6", StatusServer.resolveColor("nggak-dikenal"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}