package tech.kayys.wayang.sdk.agent;
import tech.kayys.wayang.sdk.agent.PermissionDecision;

/** User's response to a tool permission prompt. */
public enum PermissionDecision {
    APPROVE_ONCE,
    APPROVE_ALWAYS_THIS_TOOL,
    DENY
}
