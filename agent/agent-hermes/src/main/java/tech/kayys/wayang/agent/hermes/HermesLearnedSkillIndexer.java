package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * Extension point for indexing persisted Hermes-learned skills into retrieval systems.
 */
public interface HermesLearnedSkillIndexer {

    Uni<HermesLearningIndexingReceipt> index(HermesLearningIndexingRequest request);

    default Map<String, Object> toMetadata() {
        return Map.of(
                "adapterId", adapterId(),
                "adapterType", getClass().getName(),
                "ready", true);
    }

    default String adapterId() {
        return getClass().getSimpleName();
    }

    static HermesLearnedSkillIndexer noop() {
        return NoopHermesLearnedSkillIndexer.INSTANCE;
    }

    /**
     * No-op indexer used when no retrieval or vector backend is configured.
     */
    final class NoopHermesLearnedSkillIndexer implements HermesLearnedSkillIndexer {

        private static final NoopHermesLearnedSkillIndexer INSTANCE = new NoopHermesLearnedSkillIndexer();

        private NoopHermesLearnedSkillIndexer() {
        }

        @Override
        public Uni<HermesLearningIndexingReceipt> index(HermesLearningIndexingRequest request) {
            HermesLearningIndexingRequest resolved = request == null
                    ? new HermesLearningIndexingRequest(null, null, null)
                    : request;
            String reason = resolved.persistedSkill()
                    ? "no learned-skill indexer configured"
                    : "skill was not persisted";
            return Uni.createFrom().item(HermesLearningIndexingReceipt.skipped(
                    resolved,
                    reason,
                    adapterId()));
        }

        @Override
        public Map<String, Object> toMetadata() {
            return Map.of(
                    "adapterId", adapterId(),
                    "adapterType", getClass().getName(),
                    "ready", false,
                    "noop", true);
        }

        @Override
        public String adapterId() {
            return "noop";
        }
    }
}
