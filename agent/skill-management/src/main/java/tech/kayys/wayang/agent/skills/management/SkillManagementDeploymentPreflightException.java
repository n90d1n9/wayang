package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Raised when deployment preflight finds a non-deployable configuration.
 */
public class SkillManagementDeploymentPreflightException extends SkillManagementPreflightException {

    private final SkillManagementDeploymentPreflightReport report;

    public SkillManagementDeploymentPreflightException(SkillManagementDeploymentPreflightReport report) {
        super(SkillManagementEventOperation.DEPLOYMENT, Objects.requireNonNull(report, "report").validation());
        this.report = Objects.requireNonNull(report, "report");
    }

    public SkillManagementDeploymentPreflightReport report() {
        return report;
    }
}
