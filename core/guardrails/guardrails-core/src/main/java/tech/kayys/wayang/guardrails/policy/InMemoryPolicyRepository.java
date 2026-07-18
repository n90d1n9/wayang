package tech.kayys.wayang.guardrails.policy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class InMemoryPolicyRepository implements PolicyRepository {

    // Simple in-memory storage mapping policy ID to Policy
    private final Map<String, Policy> policies = new ConcurrentHashMap<>();

    public Uni<List<Policy>> findActivePolices(String tenantId, CheckPhase phase) {
        return Uni.createFrom().item(
                policies.values().stream()
                        .filter(p -> p.phase() == phase || p.phase() == null)
                        .collect(Collectors.toList()));
    }

    @Override
    public Uni<Policy> save(Policy policy) {
        policies.put(policy.id(), policy);
        return Uni.createFrom().item(policy);
    }

    @Override
    public Uni<Optional<Policy>> findById(String id) {
        return Uni.createFrom().item(Optional.ofNullable(policies.get(id)));
    }

    @Override
    public Uni<Boolean> deleteById(String id) {
        return Uni.createFrom().item(policies.remove(id) != null);
    }

    @Override
    public Uni<List<Policy>> findAll(String tenantId) {
        return Uni.createFrom().item(List.copyOf(policies.values()));
    }
}
