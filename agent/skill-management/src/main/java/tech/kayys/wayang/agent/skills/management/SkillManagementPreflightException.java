package tech.kayys.wayang.agent.skills.management;

import java.util.Locale;
import java.util.Objects;

/**
 * Base exception for operation-aware skill-management preflight failures.
 */
public class SkillManagementPreflightException extends IllegalStateException {

    private final SkillManagementEventOperation operation;
    private final SkillManagementPreflightReport preflightReport;

    public SkillManagementPreflightException(
            SkillManagementEventOperation operation,
            SkillManagementPreflightReport preflightReport) {
        super(message(operation, preflightReport));
        this.operation = Objects.requireNonNull(operation, "operation");
        this.preflightReport = SkillManagementPreflightReport.orEmpty(preflightReport);
    }

    public SkillManagementEventOperation operation() {
        return operation;
    }

    public SkillManagementPreflightReport preflightReport() {
        return preflightReport;
    }

    private static String message(
            SkillManagementEventOperation operation,
            SkillManagementPreflightReport preflightReport) {
        SkillManagementEventOperation resolvedOperation = Objects.requireNonNull(operation, "operation");
        SkillManagementPreflightReport resolvedReport = SkillManagementPreflightReport.orEmpty(preflightReport);
        String prefix = "Skill-management " + operationName(resolvedOperation) + " preflight failed";
        String errors = resolvedReport.errorsMessage();
        return errors.isBlank() ? prefix : prefix + ": " + errors;
    }

    private static String operationName(SkillManagementEventOperation operation) {
        return operation.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
