package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Encodes and decodes maintenance step diagnostics in event attribute maps.
 */
final class SkillManagementMaintenanceStepEventAttributes {

    private static final String PREFIX = "maintenanceStep.";
    private static final String STATUS = "status";
    private static final String DRY_RUN = "dryRun";
    private static final String SKIPPED = "skipped";
    private static final String CHANGED = "changed";
    private static final String CONSISTENT = "consistent";
    private static final String CHANGES = "changes";
    private static final String CONFLICTS = "conflicts";
    private static final String FAILURE = "failure";

    private SkillManagementMaintenanceStepEventAttributes() {
    }

    static void put(
            LinkedHashMap<String, String> attributes,
            List<SkillManagementMaintenanceStepDiagnostic> diagnostics) {
        SkillManagementValueSupport.nonNullList(diagnostics)
                .forEach(diagnostic -> put(attributes, diagnostic));
    }

    static List<SkillManagementAdminMaintenanceStepReport> adminReports(
            SkillManagementEventAttributeReader attributes) {
        SkillManagementEventAttributeReader reader = SkillManagementEventAttributeReader.orEmpty(attributes);
        return List.of(SkillManagementMaintenanceStep.values()).stream()
                .filter(step -> !reader.text(key(step, STATUS)).isBlank())
                .map(step -> adminReport(reader, step))
                .toList();
    }

    private static void put(
            LinkedHashMap<String, String> attributes,
            SkillManagementMaintenanceStepDiagnostic diagnostic) {
        attributes.put(key(diagnostic.step(), STATUS), diagnostic.status().name());
        attributes.put(key(diagnostic.step(), DRY_RUN), String.valueOf(diagnostic.dryRun()));
        attributes.put(key(diagnostic.step(), SKIPPED), String.valueOf(diagnostic.skipped()));
        attributes.put(key(diagnostic.step(), CHANGED), String.valueOf(diagnostic.changed()));
        attributes.put(key(diagnostic.step(), CONSISTENT), String.valueOf(diagnostic.consistent()));
        attributes.put(key(diagnostic.step(), CHANGES), String.valueOf(diagnostic.changes()));
        attributes.put(key(diagnostic.step(), CONFLICTS), String.valueOf(diagnostic.conflicts()));
        if (!diagnostic.failure().isBlank()) {
            attributes.put(key(diagnostic.step(), FAILURE), diagnostic.failure());
        }
    }

    private static SkillManagementAdminMaintenanceStepReport adminReport(
            SkillManagementEventAttributeReader attributes,
            SkillManagementMaintenanceStep step) {
        String status = attributes.text(key(step, STATUS));
        return new SkillManagementAdminMaintenanceStepReport(
                step.id(),
                status,
                attributes.flag(key(step, DRY_RUN)),
                attributes.flag(key(step, SKIPPED)),
                attributes.flag(key(step, CHANGED)),
                attributes.flag(key(step, CONSISTENT)),
                attributes.count(key(step, CHANGES)),
                attributes.count(key(step, CONFLICTS)),
                attributes.text(key(step, FAILURE)));
    }

    private static String key(SkillManagementMaintenanceStep step, String name) {
        return PREFIX + step.id() + "." + name;
    }
}
