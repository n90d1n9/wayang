package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDiagnosticsDirectiveMapperTest {

    private final HermesDiagnosticsDirectiveMapper mapper = new HermesDiagnosticsDirectiveMapper();

    @Test
    void mapsBlankViewToSummaryDirective() {
        HermesRuntimeDiagnosticsDirective directive = mapper.directive(new HermesDiagnosticsRequest(" "));

        assertThat(directive.view()).isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY);
        assertThat(directive.target()).isEqualTo("runtime-diagnostics:summary");
    }

    @Test
    void mapsSelectedViewToInspectDirective() {
        HermesRuntimeDiagnosticsDirective directive = mapper.directive(new HermesDiagnosticsRequest("runtime-ports"));

        assertThat(directive.view()).isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS);
        assertThat(directive.target()).isEqualTo("runtime-diagnostics:runtime-ports");
    }

    @Test
    void mapsNullRequestToSummaryDirective() {
        HermesRuntimeDiagnosticsDirective directive = mapper.directive(null);

        assertThat(directive.view()).isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY);
        assertThat(directive.target()).isEqualTo("runtime-diagnostics:summary");
    }

    @Test
    void mapsConvenienceViewsToDedicatedDirectives() {
        assertThat(mapper.capabilities().view())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_CAPABILITIES);
        assertThat(mapper.lifecycle().view())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE);
        assertThat(mapper.runtimePorts().view())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS);
        assertThat(mapper.skillPersistence().view())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE);
        assertThat(mapper.learningAudit().view())
                .isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT);
    }

    @Test
    void mapsLearningAuditViewAliasToDedicatedDirective() {
        HermesRuntimeDiagnosticsDirective directive = mapper.directive(new HermesDiagnosticsRequest("learning_audit"));

        assertThat(directive.view()).isEqualTo(HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT);
        assertThat(directive.target()).isEqualTo("runtime-diagnostics:learning-audit");
    }
}
