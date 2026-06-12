package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SkillStoreCapabilitiesTest {

    @Test
    void exposesStableCapabilityNamesInEnumOrder() {
        SkillStoreCapabilities capabilities = SkillStoreCapabilities.of(
                SkillStoreCapability.PRUNE_EVENTS,
                SkillStoreCapability.WRITE,
                SkillStoreCapability.QUERY_EVENTS);

        assertThat(capabilities.names()).containsExactly("write", "query-events", "prune-events");
        assertThat(capabilities.supports(SkillStoreCapability.QUERY_EVENTS)).isTrue();
        assertThat(capabilities.supports(SkillStoreCapability.DELETE)).isFalse();
    }

    @Test
    void defensivelyCopiesCapabilitySets() {
        SkillStoreCapabilities capabilities = new SkillStoreCapabilities(Set.of(
                SkillStoreCapability.READ,
                SkillStoreCapability.LIST));

        assertThat(capabilities.with(SkillStoreCapability.DELETE).names())
                .containsExactly("read", "delete", "list");
        assertThat(capabilities.names()).containsExactly("read", "list");
    }

    @Test
    void eventStoreCapabilitiesRespectEffectivePruneSupport() {
        SkillManagementEventPruner disabledPruner = new SkillManagementEventPruner() {
            @Override
            public SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
                return SkillManagementEventPruneResult.failure(options, "disabled");
            }

            @Override
            public boolean supportsPruning() {
                return false;
            }
        };

        assertThat(SkillStoreCapabilities.eventStore(new InMemorySkillManagementEventSink()).names())
                .contains("prune-events");
        assertThat(SkillStoreCapabilities.eventStore(disabledPruner).names())
                .doesNotContain("prune-events");
    }

    @Test
    void validatesCapabilityRequirementsAgainstCapabilitySets() {
        SkillStoreCapabilityRequirement requirement = SkillStoreCapabilityRequirement.eventStorePruning();

        assertThat(requirement.validate(SkillStoreCapabilities.of(SkillStoreCapability.PRUNE_EVENTS))
                .validConfiguration()).isTrue();
        assertThat(requirement.validate(SkillStoreCapabilities.none()).errors()).containsExactly(
                "Event history pruning requires an event store with capability: prune-events");
    }
}
