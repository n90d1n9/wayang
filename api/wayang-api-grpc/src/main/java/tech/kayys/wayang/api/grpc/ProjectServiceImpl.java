package tech.kayys.wayang.api.grpc;

import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.sdk.gollek.ProjectStore;
import tech.kayys.wayang.sdk.gollek.model.Project;
import tech.kayys.wayang.sdk.gollek.model.Session;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectServiceImpl implements ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);
    private final ProjectStore store;

    public ProjectServiceImpl() {
        try {
            this.store = new ProjectStore(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ProjectStore", e);
        }
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ProjectResponse> addProject(tech.kayys.wayang.api.grpc.AddProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                String dir = request.getDirectory();
                Path p = (dir == null || dir.isBlank()) ? Paths.get(System.getProperty("user.dir")) : Paths.get(dir);
                // Use name as id initially (caller can rename later)
                Project pr = store.createProject(request.getName(), request.getName(), p.toString());
                tech.kayys.wayang.api.grpc.Project proto = tech.kayys.wayang.api.grpc.Project.newBuilder()
                        .setId(pr.id())
                        .setName(pr.name())
                        .setDirectory(pr.directory() == null ? "" : pr.directory())
                        .setCreatedAt(pr.createdAt() == null ? 0L : pr.createdAt().toEpochMilli())
                        .build();
                return tech.kayys.wayang.api.grpc.ProjectResponse.newBuilder().setProject(proto).build();
            } catch (RuntimeException e) {
                log.error("addProject failed", e);
                throw e;
            } catch (Exception e) {
                log.error("addProject failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.OperationResponse> removeProject(tech.kayys.wayang.api.grpc.RemoveProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                store.removeProject(request.getId());
                return tech.kayys.wayang.api.grpc.OperationResponse.newBuilder().setOk(true).setMessage("removed").build();
            } catch (RuntimeException e) {
                log.error("removeProject failed", e);
                return tech.kayys.wayang.api.grpc.OperationResponse.newBuilder().setOk(false).setMessage(e.getMessage()).build();
            } catch (Exception e) {
                log.error("removeProject failed", e);
                return tech.kayys.wayang.api.grpc.OperationResponse.newBuilder().setOk(false).setMessage(e.getMessage()).build();
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ProjectResponse> renameProject(tech.kayys.wayang.api.grpc.RenameProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                store.renameProject(request.getId(), request.getNewName());
                tech.kayys.wayang.api.grpc.Project p = tech.kayys.wayang.api.grpc.Project.newBuilder().setId(request.getId()).setName(request.getNewName()).build();
                return tech.kayys.wayang.api.grpc.ProjectResponse.newBuilder().setProject(p).build();
            } catch (RuntimeException e) {
                log.error("renameProject failed", e);
                throw e;
            } catch (Exception e) {
                log.error("renameProject failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ProjectListResponse> listProjects(tech.kayys.wayang.api.grpc.Empty request) {
        return Uni.createFrom().item(() -> {
            try {
                List<Project> list = store.listProjects();
                tech.kayys.wayang.api.grpc.ProjectListResponse.Builder b = tech.kayys.wayang.api.grpc.ProjectListResponse.newBuilder();
                for (Project pr : list) {
                    tech.kayys.wayang.api.grpc.Project proto = tech.kayys.wayang.api.grpc.Project.newBuilder()
                            .setId(pr.id())
                            .setName(pr.name())
                            .setDirectory(pr.directory() == null ? "" : pr.directory())
                            .setCreatedAt(pr.createdAt() == null ? 0L : pr.createdAt().toEpochMilli())
                            .build();
                    b.addProjects(proto);
                }
                return b.build();
            } catch (RuntimeException e) {
                log.error("listProjects failed", e);
                throw e;
            } catch (Exception e) {
                log.error("listProjects failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ProjectResponse> switchProject(tech.kayys.wayang.api.grpc.SwitchProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                store.switchProject(request.getId());
                // Return the current project metadata
                String current = store.currentProject();
                Project found = store.listProjects().stream().filter(p -> p.id().equals(current)).findFirst().orElse(null);
                if (found == null) return tech.kayys.wayang.api.grpc.ProjectResponse.newBuilder().build();
                tech.kayys.wayang.api.grpc.Project proto = tech.kayys.wayang.api.grpc.Project.newBuilder().setId(found.id()).setName(found.name()).setDirectory(found.directory() == null ? "" : found.directory()).build();
                return tech.kayys.wayang.api.grpc.ProjectResponse.newBuilder().setProject(proto).build();
            } catch (RuntimeException e) {
                log.error("switchProject failed", e);
                throw e;
            } catch (Exception e) {
                log.error("switchProject failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ExportProjectResponse> exportProject(tech.kayys.wayang.api.grpc.ExportProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("wayang-export-", ".json");
                store.exportProject(request.getId(), tmp);
                byte[] data = java.nio.file.Files.readAllBytes(tmp);
                tech.kayys.wayang.api.grpc.ExportProjectResponse resp = tech.kayys.wayang.api.grpc.ExportProjectResponse.newBuilder().setArchive(com.google.protobuf.ByteString.copyFrom(data)).build();
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
                return resp;
            } catch (RuntimeException e) {
                log.error("exportProject failed", e);
                throw e;
            } catch (Exception e) {
                log.error("exportProject failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.ProjectResponse> importProject(tech.kayys.wayang.api.grpc.ImportProjectRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                java.nio.file.Path tmp = java.nio.file.Files.createTempFile("wayang-import-", ".json");
                java.nio.file.Files.write(tmp, request.getArchive().toByteArray());
                Project p = store.importProject(tmp);
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
                tech.kayys.wayang.api.grpc.Project proto = tech.kayys.wayang.api.grpc.Project.newBuilder().setId(p.id()).setName(p.name()).setDirectory(p.directory() == null ? "" : p.directory()).build();
                return tech.kayys.wayang.api.grpc.ProjectResponse.newBuilder().setProject(proto).build();
            } catch (RuntimeException e) {
                log.error("importProject failed", e);
                throw e;
            } catch (Exception e) {
                log.error("importProject failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.SessionListResponse> listSessions(tech.kayys.wayang.api.grpc.ListSessionsRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                List<String> ids = store.listSessions(request.getProjectId());
                tech.kayys.wayang.api.grpc.SessionListResponse.Builder b = tech.kayys.wayang.api.grpc.SessionListResponse.newBuilder();
                for (String sid : ids) {
                    tech.kayys.wayang.api.grpc.Session s = tech.kayys.wayang.api.grpc.Session.newBuilder().setId(sid).setName(sid).setProjectId(request.getProjectId()).build();
                    b.addSessions(s);
                }
                return b.build();
            } catch (RuntimeException e) {
                log.error("listSessions failed", e);
                throw e;
            } catch (Exception e) {
                log.error("listSessions failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.SessionResponse> createSession(tech.kayys.wayang.api.grpc.CreateSessionRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                Session s = store.createSession(request.getProjectId(), request.getName());
                tech.kayys.wayang.api.grpc.Session proto = tech.kayys.wayang.api.grpc.Session.newBuilder().setId(s.id()).setName(s.name()).setProjectId(request.getProjectId()).setCreatedAt(s.createdAt() == null ? 0L : s.createdAt().toEpochMilli()).build();
                return tech.kayys.wayang.api.grpc.SessionResponse.newBuilder().setSession(proto).build();
            } catch (RuntimeException e) {
                log.error("createSession failed", e);
                throw e;
            } catch (Exception e) {
                log.error("createSession failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.OperationResponse> removeSession(tech.kayys.wayang.api.grpc.RemoveSessionRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                boolean ok = store.deleteSession(request.getProjectId(), request.getSessionId());
                return tech.kayys.wayang.api.grpc.OperationResponse.newBuilder().setOk(ok).setMessage(ok ? "deleted" : "not found").build();
            } catch (RuntimeException e) {
                log.error("removeSession failed", e);
                throw e;
            } catch (Exception e) {
                log.error("removeSession failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Uni<tech.kayys.wayang.api.grpc.SessionResponse> renameSession(tech.kayys.wayang.api.grpc.RenameSessionRequest request) {
        return Uni.createFrom().item(() -> {
            try {
                // ProjectStore doesn't expose direct rename; clone session into a new one with desired name and remove old
                Session newSession = store.cloneSession(request.getProjectId(), request.getSessionId(), request.getNewName(), null);
                boolean deleted = store.deleteSession(request.getProjectId(), request.getSessionId());
                tech.kayys.wayang.api.grpc.Session proto = tech.kayys.wayang.api.grpc.Session.newBuilder().setId(newSession.id()).setName(newSession.name()).setProjectId(request.getProjectId()).setCreatedAt(newSession.createdAt() == null ? 0L : newSession.createdAt().toEpochMilli()).build();
                return tech.kayys.wayang.api.grpc.SessionResponse.newBuilder().setSession(proto).build();
            } catch (RuntimeException e) {
                log.error("renameSession failed", e);
                throw e;
            } catch (Exception e) {
                log.error("renameSession failed", e);
                throw new RuntimeException(e);
            }
        });
    }
}
