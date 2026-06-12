package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentRequest;

/**
 * Memory boundary used by Hermes mode without binding it to a concrete memory
 * backend.
 */
@FunctionalInterface
public interface HermesMemorySnapshotProvider {

    Uni<HermesMemorySnapshot> snapshot(AgentRequest request);

    static HermesMemorySnapshotProvider none() {
        return request -> Uni.createFrom().item(HermesMemorySnapshot.empty());
    }
}
