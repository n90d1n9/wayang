package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsPersistenceStatusServiceTest {

    @Test
    void buildsDefaultStatusReport() {
        SkillsPersistenceStatusReport report = service().report(null);

        assertThat(report.source()).isEqualTo("default");
        assertThat(report.profile()).isEmpty();
        assertThat(report.runtime()).isFalse();
        assertThat(report.persistence().strategy()).isEqualTo("ephemeral");
        assertThat(report.preflightAvailable()).isFalse();
        assertThat(report.diagnosticsAvailable()).isFalse();
    }

    @Test
    void statusRequestDefaultsToDefaultSource() {
        SkillsPersistenceStatusRequest request = SkillsPersistenceStatusRequest.defaults();

        assertThat(request.profileName()).isEmpty();
        assertThat(request.runtimeConfig()).isFalse();
        assertThat(request.includePreflight()).isFalse();
        assertThat(request.includeDiagnostics()).isFalse();
    }

    @Test
    void statusRequestNormalizesProfileName() {
        SkillsPersistenceStatusRequest request = SkillsPersistenceStatusRequest.fromOptions(
                " rustfs ",
                false,
                true,
                true);

        assertThat(request.profileName()).isEqualTo("rustfs");
        assertThat(request.runtimeConfig()).isFalse();
        assertThat(request.includePreflight()).isTrue();
        assertThat(request.includeDiagnostics()).isTrue();
    }

    @Test
    void buildsDefaultStatusReportWithDiagnosticsAndPreflight() {
        SkillsPersistenceStatusReport report = service().report(SkillsPersistenceStatusRequest.fromOptions(
                "",
                false,
                true,
                true));

        assertThat(report.source()).isEqualTo("default");
        assertThat(report.preflightAvailable()).isTrue();
        assertThat(report.preflight().ready()).isTrue();
        assertThat(report.diagnosticsAvailable()).isTrue();
        assertThat(report.diagnostics().stores())
                .extracting(SkillsPersistenceConfigDiagnostics.Store::kind)
                .containsExactly("registry", "memory", "none", "memory");
    }

    @Test
    void buildsProfileStatusReportWithDiagnostics() {
        SkillsPersistenceStatusReport report = service().report(SkillsPersistenceStatusRequest.fromOptions(
                "hybrid",
                false,
                false,
                true));

        assertThat(report.source()).isEqualTo("profile");
        assertThat(report.profile()).isEqualTo("hybrid-object-file");
        assertThat(report.persistence().strategy()).isEqualTo("hybrid-fallback");
        assertThat(report.preflightAvailable()).isFalse();
        assertThat(report.diagnosticsAvailable()).isTrue();
        assertThat(report.diagnostics().stores())
                .extracting(SkillsPersistenceConfigDiagnostics.Store::kind)
                .containsExactly("hybrid", "hybrid", "hybrid", "hybrid");
    }

    @Test
    void propagatesSourceResolutionErrors() {
        assertThatThrownBy(() -> service().report(SkillsPersistenceStatusRequest.fromOptions(
                "hybrid",
                true,
                false,
                false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose only one skill persistence config source: --profile or --runtime.");
    }

    private SkillsPersistenceStatusService service() {
        return new SkillsPersistenceStatusService(
                SkillManagementServiceConfig.defaults(),
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry()));
    }
}
