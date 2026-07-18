package tech.kayys.wayang.guardrails.plugin.api;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.WayangPlugin;

/**
 * Interface for guardrail policy plugins.
 */
public interface GuardrailPolicyPlugin extends WayangPlugin {

    /**
     * Evaluate the policy against the given context.
     * 
     * @param context The node execution context
     * @return A PolicyCheckResult indicating whether the policy passes or fails
     */
    Uni<PolicyCheckResult> evaluate(NodeContext context);

    /**
     * Get the category of this policy.
     */
    String getCategory();

    /**
     * Get the phases when this policy should be applied.
     */
    CheckPhase[] applicablePhases();
}
