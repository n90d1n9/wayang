package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

public class ShmLifecycleIntegrationTest {

    @Test
    public void testAllocateAndReleaseHttp() throws Exception {
        HttpServer srv = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        srv.createContext("/allocate", new HttpHandler() {
            @Override public void handle(HttpExchange ex) {
                try {
                    byte[] body = ex.getRequestBody().readAllBytes();
                    Path backing = Files.createTempFile("yaff-test-", ".dat");
                    try (FileChannel fc = FileChannel.open(backing, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                        fc.truncate(body.length);
                        MappedByteBuffer mb = fc.map(FileChannel.MapMode.READ_WRITE, 0, body.length);
                        mb.put(body);
                        mb.force();
                    }
                    String id = UUID.randomUUID().toString();
                    byte[] resp = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of("id", id, "path", backing.toString(), "offset", 0, "length", body.length));
                    ex.getResponseHeaders().set("Content-Type", "application/json");
                    ex.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
                } catch (Throwable t) {
                    try { ex.sendResponseHeaders(500, 0); ex.getResponseBody().close(); } catch (Exception ignored) {}
                }
            }
        });
        srv.createContext("/release", new HttpHandler() {
            @Override public void handle(HttpExchange ex) {
                try {
                    byte[] body = ex.getRequestBody().readAllBytes();
                    java.util.Map m = new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, java.util.Map.class);
                    String id = (String) m.get("id");
                    byte[] resp = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of("ok", true, "message", "deleted"));
                    ex.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = ex.getResponseBody()) { os.write(resp); }
                } catch (Throwable t) {
                    try { ex.sendResponseHeaders(500, 0); ex.getResponseBody().close(); } catch (Exception ignored) {}
                }
            }
        });
        srv.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(2));
        srv.start();

        int port = srv.getAddress().getPort();
        String ctrl = "http://127.0.0.1:" + port;
        byte[] payload = "hello-yaff".getBytes();
        WayangYaffFrame frame = WayangYaffFrame.allocate(ctrl, payload);
        assertNotNull(frame);
        assertNotNull(frame.id());
        assertTrue(frame.length() > 0);
        frame.close();
        // verify release via control client
        ControlPlaneClient client = new ControlPlaneClient(ctrl);
        ControlPlaneClient.ReleaseResponse rr = client.release(frame.id());
        assertTrue(rr != null && rr.ok);

        srv.stop(1);
    }
}
