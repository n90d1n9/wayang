package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Stable admin-facing projection of one validation bucket.
 */
public record SkillManagementAdminValidationReport(
        boolean valid,
        int errorCount,
        String message,
        List<String> errors) {

    public SkillManagementAdminValidationReport(List<String> errors) {
        this(false, 0, "", errors);
    }

    public SkillManagementAdminValidationReport {
        errors = SkillManagementAdminValueSupport.compactStrings(errors);
        valid = errors.isEmpty();
        errorCount = errors.size();
        message = SkillManagementAdminValueSupport.joinedMessage(errors);
    }
}
