package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDiagnosticsViewMapperTest {

    private final HermesDiagnosticsViewMapper mapper = new HermesDiagnosticsViewMapper();

    @Test
    void mapsNullAndBlankRequestsToSummary() {
        assertThat(mapper.view(null))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY);
        assertThat(mapper.view(new HermesDiagnosticsRequest(" ")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY);
    }

    @Test
    void normalizesKnownViews() {
        assertThat(mapper.view(new HermesDiagnosticsRequest("Runtime_Ports")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS);
        assertThat(mapper.view(new HermesDiagnosticsRequest(" LifeCycle ")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE);
        assertThat(mapper.view(new HermesDiagnosticsRequest(" skill-persistence ")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE);
        assertThat(mapper.view(new HermesDiagnosticsRequest(" learning_audit ")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT);
    }

    @Test
    void mapsUnknownViewToFull() {
        assertThat(mapper.view(new HermesDiagnosticsRequest("unknown")))
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_FULL);
    }

    @Test
    void mapsDedicatedViews() {
        assertThat(mapper.capabilities())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_CAPABILITIES);
        assertThat(mapper.lifecycle())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE);
        assertThat(mapper.runtimePorts())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS);
        assertThat(mapper.skillPersistence())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE);
        assertThat(mapper.learningAudit())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT);
    }
}
