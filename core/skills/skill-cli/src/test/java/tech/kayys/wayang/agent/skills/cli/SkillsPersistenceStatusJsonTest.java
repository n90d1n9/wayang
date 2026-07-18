package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminDeploymentPreflightReport;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceRole;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminValidationReport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceStatusJsonTest {

    @Test
    void rendersStrategyAndEscapesStrings() {
        SkillManagementAdminPersistenceRole role = new SkillManagementAdminPersistenceRole(
                "definition",
                "definition",
                "custom",
                "custom",
                "custom",
                false,
                true,
                false,
                false,
                true,
                true,
                false,
                false,
                List.of("read", "write"),
                List.of());
        SkillManagementAdminPersistenceStrategy strategy = new SkillManagementAdminPersistenceStrategy(
                "custom",
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                List.of("Custom \"store\" needs\\contract"),
                List.of(role));

        String json = SkillsPersistenceStatusJson.toJson(strategy);

        assertThat(json)
                .startsWith("{")
                .endsWith("}")
                .contains("\"strategy\":\"custom\"")
                .doesNotContain("\"source\":")
                .contains("\"hasCustomProvider\":true")
                .contains("\"ephemeralRoleCount\":1")
                .contains("\"warnings\":[\"Custom \\\"store\\\" needs\\\\contract\"]")
                .contains("\"roles\":[{\"role\":\"definition\"")
                .contains("\"capabilities\":[\"read\",\"write\"]")
                .contains("\"children\":[]");
    }

    @Test
    void rendersReportSourceMetadata() {
        SkillManagementAdminPersistenceStrategy strategy = new SkillManagementAdminPersistenceStrategy(
                "ephemeral",
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                List.of("Ephemeral skill persistence roles: definition"),
                List.of());

        String json = SkillsPersistenceStatusJson.toJson(new SkillsPersistenceStatusReport(
                "profile",
                "default",
                false,
                strategy));

        assertThat(json)
                .startsWith("{")
                .contains("\"source\":\"profile\"")
                .contains("\"profile\":\"default\"")
                .contains("\"runtime\":false")
                .contains("\"preflightAvailable\":false")
                .contains("\"diagnosticsAvailable\":false")
                .contains("\"strategy\":\"ephemeral\"")
                .contains("\"warnings\":[\"Ephemeral skill persistence roles: definition\"]")
                .contains("\"roles\":[]");
    }

    @Test
    void rendersReportPreflightMetadata() {
        SkillManagementAdminPersistenceStrategy strategy = new SkillManagementAdminPersistenceStrategy(
                "object-storage",
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                List.of(),
                List.of());
        SkillManagementAdminValidationReport valid = new SkillManagementAdminValidationReport(List.of());
        SkillManagementAdminDeploymentPreflightReport preflight =
                new SkillManagementAdminDeploymentPreflightReport(
                        valid,
                        new SkillManagementAdminValidationReport(List.of("Object store missing")),
                        valid,
                        valid);

        String json = SkillsPersistenceStatusJson.toJson(new SkillsPersistenceStatusReport(
                "profile",
                "object-storage",
                false,
                strategy,
                preflight));

        assertThat(json)
                .contains("\"preflightAvailable\":true")
                .contains("\"diagnosticsAvailable\":false")
                .contains("\"preflight\":{\"ready\":false")
                .contains("\"deployable\":false")
                .contains("\"errorCount\":1")
                .contains("\"errors\":[\"Object store missing\"]")
                .contains("\"targetStores\":{\"valid\":false")
                .contains("\"strategy\":\"object-storage\"");
    }

    @Test
    void rendersReportDiagnosticsMetadata() {
        SkillManagementAdminPersistenceStrategy strategy = new SkillManagementAdminPersistenceStrategy(
                "ephemeral",
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of());
        SkillsPersistenceConfigDiagnostics diagnostics = new SkillsPersistenceConfigDiagnostics(
                "create-missing",
                true,
                false,
                List.of(new SkillsPersistenceConfigDiagnostics.Store(
                        "event-history",
                        "memory",
                        "memory",
                        false,
                        10,
                        List.of())));

        String json = SkillsPersistenceStatusJson.toJson(new SkillsPersistenceStatusReport(
                "runtime",
                "",
                true,
                strategy,
                null,
                diagnostics));

        assertThat(json)
                .contains("\"diagnosticsAvailable\":true")
                .contains("\"diagnostics\":{\"lifecycleReconcile\":\"create-missing\"")
                .contains("\"createMissingStates\":true")
                .contains("\"removeOrphanedStates\":false")
                .contains("\"stores\":[{\"role\":\"event-history\",\"kind\":\"memory\"")
                .contains("\"maxEvents\":10")
                .contains("\"children\":[]");
    }
}
