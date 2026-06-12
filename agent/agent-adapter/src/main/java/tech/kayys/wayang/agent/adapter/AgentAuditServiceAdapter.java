package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.audit.AgentArtifact;
import tech.kayys.wayang.agent.spi.audit.AgentAuditService;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Small in-memory {@link AgentAuditService} adapter for standalone runtimes and
 * tests.
 *
 * <p>
 * This class is intentionally not a CDI bean. Production runtimes can bind a
 * durable audit implementation without this module creating a second competing
 * {@link AgentAuditService} bean.
 */
public class AgentAuditServiceAdapter implements AgentAuditService {

    private final ConcurrentMap<String, AgentArtifact> artifacts = new ConcurrentHashMap<>();

    @Override
    public Uni<Void> saveArtifact(AgentArtifact artifact) {
        if (artifact == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Artifact must not be null"));
        }
        artifacts.put(key(artifact.tenantId(), artifact.id()), artifact);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<AgentArtifact> getArtifact(String artifactId, String tenantId) {
        return Uni.createFrom().item(() -> artifacts.get(key(tenantId, artifactId)));
    }

    @Override
    public Uni<List<AgentArtifact>> getArtifactsByRun(String runId, String tenantId) {
        return Uni.createFrom().item(() -> artifacts.values().stream()
                .filter(artifact -> Objects.equals(tenantId, artifact.tenantId()))
                .filter(artifact -> Objects.equals(runId, artifact.runId()))
                .sorted(Comparator.comparing(AgentArtifact::createdAt))
                .toList());
    }

    public int size() {
        return artifacts.size();
    }

    public void clear() {
        artifacts.clear();
    }

    private String key(String tenantId, String artifactId) {
        return (tenantId == null ? "" : tenantId) + ":" + (artifactId == null ? "" : artifactId);
    }
}
