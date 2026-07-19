package tech.kayys.wayang.api.grpc;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: This is a lightweight skeleton. The generated classes from the proto
// (WayangApiProto, ProjectServiceGrpc, etc.) are expected to exist after
// protoc compilation. Implementations should bridge to Wayang's ProjectStore
// / session manager in wayang/sdk/wayang-gollek-sdk.

public class WayangProjectService extends ProjectServiceGrpc.ProjectServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(WayangProjectService.class);
    private final tech.kayys.wayang.sdk.gollek.ProjectStore store;

    public WayangProjectService() {
        super();
        tech.kayys.wayang.sdk.gollek.ProjectStore tmp = null;
        try {
            tmp = new tech.kayys.wayang.sdk.gollek.ProjectStore(null);
        } catch (Exception e) {
            log.error("Failed to initialize ProjectStore: {}", e.getMessage());
        }
        this.store = tmp;
    }

    @Override
    public void addProject(AddProjectRequest req, StreamObserver<ProjectResponse> resp) {
        log.info("gRPC AddProject: name={} dir={}", req.getName(), req.getDirectory());
        try {
            String dir = req.getDirectory();
            java.nio.file.Path path = (dir == null || dir.isBlank()) ? java.nio.file.Paths.get(System.getProperty("user.dir")) : java.nio.file.Path.of(dir);
            tech.kayys.wayang.sdk.gollek.model.Project p = store.createProject(req.getName(), req.getName(), path.toString());
            Project proto = Project.newBuilder()
                    .setId(p.id())
                    .setName(p.name())
                    .setDirectory(p.directory() == null ? "" : p.directory())
                    .setCreatedAt(p.createdAt() == null ? System.currentTimeMillis() : p.createdAt().toEpochMilli())
                    .build();
            resp.onNext(ProjectResponse.newBuilder().setProject(proto).build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("addProject failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listProjects(Empty req, StreamObserver<ProjectListResponse> resp) {
        log.info("gRPC ListProjects");
        try {
            ProjectListResponse.Builder b = ProjectListResponse.newBuilder();
            for (tech.kayys.wayang.sdk.gollek.model.Project p : store.listProjects()) {
                b.addProjects(Project.newBuilder()
                        .setId(p.id())
                        .setName(p.name())
                        .setDirectory(p.directory() == null ? "" : p.directory())
                        .setCreatedAt(p.createdAt() == null ? 0L : p.createdAt().toEpochMilli())
                        .build());
            }
            resp.onNext(b.build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("listProjects failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createSession(CreateSessionRequest req, StreamObserver<SessionResponse> resp) {
        log.info("gRPC CreateSession project={} name={}", req.getProjectId(), req.getName());
        try {
            tech.kayys.wayang.sdk.gollek.model.Session s = store.createSession(req.getProjectId(), req.getName());
            Session proto = Session.newBuilder()
                    .setId(s.id())
                    .setName(s.name())
                    .setProjectId(req.getProjectId())
                    .setCreatedAt(s.createdAt() == null ? System.currentTimeMillis() : s.createdAt().toEpochMilli())
                    .build();
            resp.onNext(SessionResponse.newBuilder().setSession(proto).build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("createSession failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listSessions(ListSessionsRequest req, StreamObserver<SessionListResponse> resp) {
        log.info("gRPC ListSessions project={}", req.getProjectId());
        try {
            SessionListResponse.Builder b = SessionListResponse.newBuilder();
            for (String sid : store.listSessions(req.getProjectId())) {
                b.addSessions(Session.newBuilder().setId(sid).setName(sid).setProjectId(req.getProjectId()).build());
            }
            resp.onNext(b.build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("listSessions failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void removeSession(RemoveSessionRequest req, StreamObserver<OperationResponse> resp) {
        log.info("gRPC RemoveSession project={} session={}", req.getProjectId(), req.getSessionId());
        try {
            boolean ok = store.deleteSession(req.getProjectId(), req.getSessionId());
            resp.onNext(OperationResponse.newBuilder().setOk(ok).setMessage(ok ? "deleted" : "not found").build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("removeSession failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void removeProject(RemoveProjectRequest req, StreamObserver<OperationResponse> resp) {
        log.info("gRPC RemoveProject id={}", req.getId());
        try {
            store.removeProject(req.getId());
            resp.onNext(OperationResponse.newBuilder().setOk(true).setMessage("removed").build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("removeProject failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void renameProject(RenameProjectRequest req, StreamObserver<ProjectResponse> resp) {
        log.info("gRPC RenameProject id={} newName={}", req.getId(), req.getNewName());
        try {
            store.renameProject(req.getId(), req.getNewName());
            // return updated project metadata
            Project.Builder p = Project.newBuilder().setId(req.getId()).setName(req.getNewName());
            resp.onNext(ProjectResponse.newBuilder().setProject(p).build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("renameProject failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void exportProject(ExportProjectRequest req, StreamObserver<ExportProjectResponse> resp) {
        log.info("gRPC ExportProject id={}", req.getId());
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("wayang-export-", ".json");
            store.exportProject(req.getId(), tmp);
            byte[] data = java.nio.file.Files.readAllBytes(tmp);
            resp.onNext(ExportProjectResponse.newBuilder().setArchive(com.google.protobuf.ByteString.copyFrom(data)).build());
            resp.onCompleted();
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("exportProject failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void importProject(ImportProjectRequest req, StreamObserver<ProjectResponse> resp) {
        log.info("gRPC ImportProject name={}", req.getName());
        try {
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("wayang-import-", ".json");
            java.nio.file.Files.write(tmp, req.getArchive().toByteArray());
            tech.kayys.wayang.sdk.gollek.model.Project p = store.importProject(tmp);
            resp.onNext(ProjectResponse.newBuilder().setProject(Project.newBuilder().setId(p.id()).setName(p.name()).setDirectory(p.directory() == null ? "" : p.directory()).build()).build());
            resp.onCompleted();
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        } catch (Exception e) {
            log.error("importProject failed: {}", e.getMessage());
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
