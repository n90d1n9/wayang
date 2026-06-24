package tech.kayys.wayang.api.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.sdk.gollek.ProjectStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class WayangServe {
    private static final Logger log = LoggerFactory.getLogger(WayangServe.class);

    public static void main(String[] args) throws Exception {
        boolean startGrpc = true;
        boolean startRest = true;
        for (String a : args) {
            if ("--grpc".equals(a)) { startRest = false; }
            if ("--rest".equals(a)) { startGrpc = false; }
            if ("--help".equals(a) || "-h".equals(a)) { System.out.println("Usage: WayangServe [--grpc] [--rest]"); return; }
        }

        ProjectStore store = new ProjectStore(null);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

        Server grpcServer = null;
        HttpServer httpServer = null;

        if (startGrpc) {
            int grpcPort = 50051;
            grpcServer = ServerBuilder.forPort(grpcPort)
                    .addService(new WayangProjectService())
                    .addService(new GrpcSessionService())
                    .addService(new GrpcSdkService())
                    .build()
                    .start();
            log.info("gRPC server started on port {}", grpcPort);
        }

        if (startRest) {
            int restPort = 8080;
            httpServer = HttpServer.create(new InetSocketAddress(restPort), 0);
            httpServer.createContext("/projects", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) {
                    try {
                        String method = exchange.getRequestMethod();
                        if ("GET".equalsIgnoreCase(method)) {
                            java.util.List<tech.kayys.wayang.sdk.gollek.model.Project> projects = store.listProjects();
                            java.util.List<java.util.Map<String,Object>> respList = new java.util.ArrayList<>();
                            for (tech.kayys.wayang.sdk.gollek.model.Project p : projects) {
                                java.util.Map<String,Object> m = new java.util.HashMap<>();
                                m.put("id", p.id());
                                m.put("name", p.name());
                                m.put("directory", p.directory());
                                respList.add(m);
                            }
                            String body = mapper.writeValueAsString(respList);
                            exchange.getResponseHeaders().add("Content-Type", "application/json");
                            byte[] out = body.getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(200, out.length);
                            try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                            return;
                        }
                        // POST create
                        if ("POST".equalsIgnoreCase(method)) {
                            InputStream is = exchange.getRequestBody();
                            java.util.Map<?,?> body = mapper.readValue(is, java.util.Map.class);
                            Object nameObj = body.get("name");
                            String name = nameObj != null ? String.valueOf(nameObj) : "project-" + System.currentTimeMillis();
                            Object dirObj = body.get("directory");
                            String dir = dirObj != null ? String.valueOf(dirObj) : System.getProperty("user.dir");
                            var pr = store.createProject(name, name, dir);
                            java.util.Map<String,String> respMap = new java.util.HashMap<>();
                            respMap.put("id", pr.id());
                            respMap.put("name", pr.name());
                            String resp = mapper.writeValueAsString(respMap);
                            exchange.getResponseHeaders().add("Content-Type", "application/json");
                            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(201, out.length);
                            try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                            return;
                        }
                        exchange.sendResponseHeaders(405, -1);
                    } catch (Exception e) {
                        try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
                    }
                }
            });
            httpServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            httpServer.start();
            log.info("REST server started on port {}", restPort);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Wayang servers...");
            try { if (httpServer != null) httpServer.stop(0); } catch (Exception ignored) {}
            try { if (grpcServer != null) grpcServer.shutdown(); } catch (Exception ignored) {}
        }));

        // block
        if (grpcServer != null) grpcServer.awaitTermination();
        if (httpServer != null) Thread.currentThread().join();
    }
}
