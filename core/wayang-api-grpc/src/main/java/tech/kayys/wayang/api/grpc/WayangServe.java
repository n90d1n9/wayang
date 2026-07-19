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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class WayangServe {
    private static final Logger log = LoggerFactory.getLogger(WayangServe.class);

    public static void main(String[] args) throws Exception {
        boolean startGrpc = true;
        boolean startRest = true;
        int grpcPort = 50051;
        int restPort = 8080;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--grpc".equals(a)) { startRest = false; }
            else if ("--rest".equals(a)) { startGrpc = false; }
            else if (a.startsWith("--grpc-port=")) {
                try { grpcPort = Integer.parseInt(a.substring("--grpc-port=".length())); } catch (NumberFormatException ignored) {}
            } else if (a.startsWith("--rest-port=")) {
                try { restPort = Integer.parseInt(a.substring("--rest-port=".length())); } catch (NumberFormatException ignored) {}
            } else if ("--help".equals(a) || "-h".equals(a)) { System.out.println("Usage: WayangServe [--grpc] [--rest] [--grpc-port=PORT] [--rest-port=PORT]"); return; }
        }

        ProjectStore store = new ProjectStore(null);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);

        final Server[] grpcHolder = new Server[1];
        final HttpServer[] httpHolder = new HttpServer[1];

        if (startGrpc) {
            Server server = null;
            try {
                server = ServerBuilder.forPort(grpcPort)
                        .addService(new WayangProjectService())
                        .addService(new GrpcSessionService())
                        .addService(new GrpcSdkService())
                        .build()
                        .start();
                grpcHolder[0] = server;
                log.info("gRPC server started on port {}", server.getPort());
            } catch (java.net.BindException be) {
                log.warn("gRPC port {} in use, retrying with ephemeral port", grpcPort);
                server = ServerBuilder.forPort(0)
                        .addService(new WayangProjectService())
                        .addService(new GrpcSessionService())
                        .addService(new GrpcSdkService())
                        .build()
                        .start();
                grpcHolder[0] = server;
                log.info("gRPC server started on ephemeral port {}", server.getPort());
            }
        }

        if (startRest) {
            HttpServer server = null;
            try {
                server = HttpServer.create(new InetSocketAddress(restPort), 0);
            } catch (java.net.BindException be) {
                log.warn("REST port {} in use, binding to ephemeral port", restPort);
                server = HttpServer.create(new InetSocketAddress(0), 0);
            }

            // /projects - list and create projects
            server.createContext("/projects", new HttpHandler() {
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
                        // POST create project
                        if ("POST".equalsIgnoreCase(method)) {
                            InputStream is = exchange.getRequestBody();
                            java.util.Map<?,?> body = mapper.readValue(is, java.util.Map.class);
                            Object nameObj = body.get("name");
                            String name = nameObj != null ? String.valueOf(nameObj) : "project-" + System.currentTimeMillis();
                            Object dirObj = body.get("directory");
                            String dir = dirObj != null ? String.valueOf(dirObj) : System.getProperty("user.dir");
                            tech.kayys.wayang.sdk.gollek.model.Project pr = store.createProject(name, name, dir);
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

            // /projects/... prefix handler for sessions, transcripts, import/export
            server.createContext("/projects/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) {
                    try {
                        String method = exchange.getRequestMethod();
                        String path = exchange.getRequestURI().getPath();
                        // path starts with /projects/
                        String suffix = path.substring("/projects/".length());
                        String[] parts = suffix.split("/");
                        if (parts.length >= 2 && "sessions".equals(parts[1])) {
                            String projectId = parts[0];
                            if (parts.length == 2) {
                                // /projects/{pid}/sessions
                                if ("GET".equalsIgnoreCase(method)) {
                                    java.util.List<String> sessions = store.listSessions(projectId);
                                    String body = mapper.writeValueAsString(sessions);
                                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                                    byte[] out = body.getBytes(StandardCharsets.UTF_8);
                                    exchange.sendResponseHeaders(200, out.length);
                                    try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                                    return;
                                }
                                if ("POST".equalsIgnoreCase(method)) {
                                    InputStream is = exchange.getRequestBody();
                                    java.util.Map<?,?> body = mapper.readValue(is, java.util.Map.class);
                                    Object nameObj = body.get("name");
                                    String name = nameObj != null ? String.valueOf(nameObj) : "session-" + System.currentTimeMillis();
                                    tech.kayys.wayang.sdk.gollek.model.Session s = store.createSession(projectId, name);
                                    String resp = mapper.writeValueAsString(java.util.Map.of("id", s.id(), "name", s.name()));
                                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                                    byte[] out = resp.getBytes(StandardCharsets.UTF_8);
                                    exchange.sendResponseHeaders(201, out.length);
                                    try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                                    return;
                                }
                            } else if (parts.length >= 3) {
                                String sessionId = parts[2];
                                // /projects/{pid}/sessions/{sid}/transcript
                                if (parts.length >= 4 && "transcript".equals(parts[3])) {
                                    if ("GET".equalsIgnoreCase(method)) {
                                    java.util.List<?> transcript = store.loadTranscript(projectId, sessionId);
                                        String body = mapper.writeValueAsString(transcript);
                                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                                        byte[] out = body.getBytes(StandardCharsets.UTF_8);
                                        exchange.sendResponseHeaders(200, out.length);
                                        try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                                        return;
                                    }
                                    if ("POST".equalsIgnoreCase(method)) {
                                        // append messages (accept single entry or array)
                                        InputStream is = exchange.getRequestBody();
                                        Object obj = mapper.readValue(is, Object.class);
                                    java.util.List<?> existing = store.loadTranscript(projectId, sessionId);
                                        java.util.List<Object> toAppend = new java.util.ArrayList<>();
                                    if (obj instanceof java.util.List) for (Object o : (java.util.List<?>) obj) toAppend.add(o);
                                        else toAppend.add(obj);
                                        java.util.List<Object> merged = new java.util.ArrayList<>();
                                    if (existing != null) merged.addAll((java.util.Collection<?>) existing);
                                        merged.addAll(toAppend);
                                        store.saveTranscript(projectId, sessionId, merged);
                                        exchange.sendResponseHeaders(204, -1);
                                        return;
                                    }
                                }
                                // /projects/{pid}/sessions/{sid}/export -> return transcript JSON
                                if (parts.length >= 4 && "export".equals(parts[3]) && "GET".equalsIgnoreCase(method)) {
                                    java.util.List<?> transcript = store.loadTranscript(projectId, sessionId);
                                    String body = mapper.writeValueAsString(transcript);
                                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=session-" + sessionId + ".json");
                                    byte[] out = body.getBytes(StandardCharsets.UTF_8);
                                    exchange.sendResponseHeaders(200, out.length);
                                    try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                                    return;
                                }
                                // /projects/{pid}/sessions/{sid}/import -> POST with { "sessionId":"...","transcript": [...] }
                                if (parts.length >= 4 && "import".equals(parts[3]) && "POST".equalsIgnoreCase(method)) {
                                    InputStream is = exchange.getRequestBody();
                                    java.util.Map<?,?> body = mapper.readValue(is, java.util.Map.class);
                                    Object sidObj = body.get("sessionId");
                                    Object transcriptObj = body.get("transcript");
                                    String newSid = sidObj != null ? String.valueOf(sidObj) : null;
                                    String createdSid;
                                    if (newSid == null || newSid.isBlank()) {
                                        tech.kayys.wayang.sdk.gollek.model.Session s = store.createSession(projectId, "imported-");
                                        createdSid = s.id();
                                    } else {
                                        createdSid = newSid;
                                    }
                                    java.util.List<?> trip = transcriptObj instanceof java.util.List ? (java.util.List<?>) transcriptObj : java.util.List.of(transcriptObj);
                                    store.saveTranscript(projectId, createdSid, (java.util.List<?>) trip);
                                    String resp = mapper.writeValueAsString(java.util.Map.of("id", createdSid));
                                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                                    byte[] out = resp.getBytes(StandardCharsets.UTF_8);
                                    exchange.sendResponseHeaders(201, out.length);
                                    try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                                    return;
                                }
                                // /projects/{pid}/sessions/{sid} DELETE -> delete session
                                if (parts.length == 3 && "DELETE".equalsIgnoreCase(method)) {
                                    boolean ok = store.deleteSession(projectId, sessionId);
                                    if (ok) exchange.sendResponseHeaders(204, -1);
                                    else exchange.sendResponseHeaders(404, -1);
                                    return;
                                }
                            }
                        }

                        // /projects/import -> import project bundle
                        if (suffix.equals("import") && "POST".equalsIgnoreCase(method)) {
                            InputStream is = exchange.getRequestBody();
                            // accept a zip or JSON project export; here accept JSON
                            Path tmp = Files.createTempFile("wayang-project-import", ".json");
                            Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            tech.kayys.wayang.sdk.gollek.model.Project imported = store.importProject(tmp);
                            String resp = mapper.writeValueAsString(java.util.Map.of("id", imported.id()));
                            exchange.getResponseHeaders().add("Content-Type", "application/json");
                            byte[] out = resp.getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(201, out.length);
                            try (OutputStream os = exchange.getResponseBody()) { os.write(out); }
                            return;
                        }

                        exchange.sendResponseHeaders(404, -1);
                    } catch (Exception e) {
                        log.warn("REST handler error", e);
                        try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
                    }
                }
            });

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            httpHolder[0] = server;
            int actual = server.getAddress() != null ? server.getAddress().getPort() : restPort;
            log.info("REST server started on port {}", actual);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Wayang servers...");
            try { if (httpHolder[0] != null) httpHolder[0].stop(0); } catch (Exception ignored) {}
            try { if (grpcHolder[0] != null) grpcHolder[0].shutdown(); } catch (Exception ignored) {}
        }));

        // block
        if (grpcHolder[0] != null) grpcHolder[0].awaitTermination();
        if (httpHolder[0] != null) Thread.currentThread().join();
    }
}
