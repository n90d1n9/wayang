package tech.kayys.wayang.guardrails.policy;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository {
    Uni<List<Policy>> findActivePolices(String tenantId, CheckPhase phase);

    Uni<Policy> save(Policy policy);

    Uni<Optional<Policy>> findById(String id);

    Uni<Boolean> deleteById(String id);

    Uni<List<Policy>> findAll(String tenantId);
}
