package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementAdminDeploymentPreflightReport;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceRole;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminPersistenceStrategy;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminValidationReport;

import java.util.List;

import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.arrayField;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.field;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.name;
import static tech.kayys.wayang.agent.skills.cli.SkillsJsonWriter.trimComma;

/**
 * JSON renderer for CLI persistence status output.
 */
final class SkillsPersistenceStatusJson {

    private SkillsPersistenceStatusJson() {
    }

    static String toJson(SkillsPersistenceStatusReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        field(builder, "source", report.source());
        field(builder, "profile", report.profile());
        field(builder, "runtime", report.runtime());
        field(builder, "preflightAvailable", report.preflightAvailable());
        preflightField(builder, report.preflight());
        field(builder, "diagnosticsAvailable", report.diagnosticsAvailable());
        SkillsPersistenceConfigDiagnosticsJson.diagnosticsField(builder, "diagnostics", report.diagnostics());
        appendStrategy(builder, report.persistence());
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    static String toJson(SkillManagementAdminPersistenceStrategy strategy) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        appendStrategy(builder, strategy);
        trimComma(builder);
        builder.append('}');
        return builder.toString();
    }

    private static void appendStrategy(
            StringBuilder builder,
            SkillManagementAdminPersistenceStrategy strategy) {
        field(builder, "strategy", strategy.strategy());
        field(builder, "fullyDurable", strategy.fullyDurable());
        field(builder, "hasEphemeralRole", strategy.hasEphemeralRole());
        field(builder, "hasDurableFallback", strategy.hasDurableFallback());
        field(builder, "hasExternalProvider", strategy.hasExternalProvider());
        field(builder, "hasCustomProvider", strategy.hasCustomProvider());
        field(builder, "hasCompositeProvider", strategy.hasCompositeProvider());
        field(builder, "hasMirroredProvider", strategy.hasMirroredProvider());
        field(builder, "roleCount", strategy.roleCount());
        field(builder, "durableRoleCount", strategy.durableRoleCount());
        field(builder, "ephemeralRoleCount", strategy.ephemeralRoleCount());
        field(builder, "disabledRoleCount", strategy.disabledRoleCount());
        field(builder, "customRoleCount", strategy.customRoleCount());
        field(builder, "warningCount", strategy.warningCount());
        arrayField(builder, "warnings", strategy.warnings());
        rolesField(builder, "roles", strategy.roles());
    }

    private static void preflightField(
            StringBuilder builder,
            SkillManagementAdminDeploymentPreflightReport preflight) {
        if (preflight == null) {
            return;
        }
        name(builder, "preflight");
        builder.append('{');
        field(builder, "ready", preflight.ready());
        field(builder, "deployable", preflight.deployable());
        field(builder, "errorCount", preflight.errorCount());
        field(builder, "message", preflight.message());
        arrayField(builder, "errors", preflight.errors());
        validationField(builder, "configuration", preflight.configuration());
        validationField(builder, "targetStores", preflight.targetStores());
        validationField(builder, "sourceStores", preflight.sourceStores());
        validationField(builder, "capabilities", preflight.capabilities());
        trimComma(builder);
        builder.append("},");
    }

    private static void validationField(
            StringBuilder builder,
            String name,
            SkillManagementAdminValidationReport validation) {
        name(builder, name);
        builder.append('{');
        field(builder, "valid", validation.valid());
        field(builder, "errorCount", validation.errorCount());
        field(builder, "message", validation.message());
        arrayField(builder, "errors", validation.errors());
        trimComma(builder);
        builder.append("},");
    }

    private static void rolesField(
            StringBuilder builder,
            String name,
            List<SkillManagementAdminPersistenceRole> roles) {
        name(builder, name);
        builder.append('[');
        for (SkillManagementAdminPersistenceRole role : roles) {
            role(builder, role);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    private static void role(StringBuilder builder, SkillManagementAdminPersistenceRole role) {
        builder.append('{');
        field(builder, "role", role.role());
        field(builder, "path", role.path());
        field(builder, "provider", role.provider());
        field(builder, "persistenceClass", role.persistenceClass());
        field(builder, "strategy", role.strategy());
        field(builder, "disabled", role.disabled());
        field(builder, "ephemeral", role.ephemeral());
        field(builder, "durable", role.durable());
        field(builder, "durableFallback", role.durableFallback());
        field(builder, "external", role.external());
        field(builder, "custom", role.custom());
        field(builder, "composite", role.composite());
        field(builder, "mirrored", role.mirrored());
        arrayField(builder, "capabilities", role.capabilities());
        rolesField(builder, "children", role.children());
        trimComma(builder);
        builder.append('}');
    }

}
