package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridSkillLifecycleStateStoreTest {

    @Test
    void readsPrimaryBeforeFallbackAndMergesSnapshots() {
        InMemorySkillLifecycleStateStore primary = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        SkillLifecycleState fallbackPlanner = state("planner", SkillLifecycleStatus.DISABLED, 1);
        SkillLifecycleState primaryPlanner = state("planner", SkillLifecycleStatus.ACTIVE, 2);
        SkillLifecycleState fallbackWriter = state("writer", SkillLifecycleStatus.DEPRECATED, 1);
        fallback.save(fallbackPlanner);
        fallback.save(fallbackWriter);
        primary.save(primaryPlanner);

        HybridSkillLifecycleStateStore hybrid = new HybridSkillLifecycleStateStore(primary, fallback);

        assertThat(hybrid.get("planner")).contains(primaryPlanner);
        assertThat(hybrid.get("writer")).contains(fallbackWriter);
        assertThat(hybrid.snapshot()).containsEntry("planner", primaryPlanner)
                .containsEntry("writer", fallbackWriter);
    }

    @Test
    void writesToPrimaryAndRemovesFromBothStores() {
        InMemorySkillLifecycleStateStore primary = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        HybridSkillLifecycleStateStore hybrid = new HybridSkillLifecycleStateStore(primary, fallback);
        SkillLifecycleState planner = state("planner", SkillLifecycleStatus.ACTIVE, 1);
        fallback.save(state("planner", SkillLifecycleStatus.DISABLED, 1));

        hybrid.save(planner);

        assertThat(primary.get("planner")).contains(planner);
        assertThat(fallback.get("planner")).isPresent();
        assertThat(hybrid.remove("planner")).isTrue();
        assertThat(primary.get("planner")).isEmpty();
        assertThat(fallback.get("planner")).isEmpty();
    }

    @Test
    void fallbackReadRepairsMissingPrimaryLifecycleState() {
        InMemorySkillLifecycleStateStore primary = new InMemorySkillLifecycleStateStore();
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        SkillLifecycleState planner = state("planner", SkillLifecycleStatus.DISABLED, 1);
        fallback.save(planner);
        HybridSkillLifecycleStateStore hybrid = new HybridSkillLifecycleStateStore(primary, fallback);

        assertThat(hybrid.get("planner")).contains(planner);

        assertThat(primary.get("planner")).contains(planner);
    }

    @Test
    void readPathsUseFallbackWhenPrimaryIsUnavailable() {
        InMemorySkillLifecycleStateStore fallback = new InMemorySkillLifecycleStateStore();
        SkillLifecycleState planner = state("planner", SkillLifecycleStatus.DISABLED, 1);
        fallback.save(planner);
        HybridSkillLifecycleStateStore hybrid = new HybridSkillLifecycleStateStore(
                new FailingReadSkillLifecycleStateStore(),
                fallback);

        assertThat(hybrid.get("planner")).contains(planner);
        assertThat(hybrid.snapshot()).containsEntry("planner", planner);
    }

    @Test
    void writesStillRequirePrimary() {
        HybridSkillLifecycleStateStore hybrid = new HybridSkillLifecycleStateStore(
                new FailingWriteSkillLifecycleStateStore(),
                new InMemorySkillLifecycleStateStore());

        assertThatThrownBy(() -> hybrid.save(state("planner", SkillLifecycleStatus.ACTIVE, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary write unavailable");
    }

    private SkillLifecycleState state(String skillId, SkillLifecycleStatus status, int revision) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new SkillLifecycleState(skillId, status, now, now, revision);
    }

    private static final class FailingReadSkillLifecycleStateStore implements SkillLifecycleStateStore {
        @Override
        public Optional<SkillLifecycleState> get(String skillId) {
            throw new IllegalStateException("primary read unavailable");
        }

        @Override
        public SkillLifecycleState save(SkillLifecycleState state) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(String skillId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, SkillLifecycleState> snapshot() {
            throw new IllegalStateException("primary read unavailable");
        }
    }

    private static final class FailingWriteSkillLifecycleStateStore implements SkillLifecycleStateStore {
        @Override
        public Optional<SkillLifecycleState> get(String skillId) {
            return Optional.empty();
        }

        @Override
        public SkillLifecycleState save(SkillLifecycleState state) {
            throw new IllegalStateException("primary write unavailable");
        }

        @Override
        public boolean remove(String skillId) {
            return false;
        }

        @Override
        public Map<String, SkillLifecycleState> snapshot() {
            return Map.of();
        }
    }
}
