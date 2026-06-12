package tech.kayys.gamelan.execution.sandbox;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Placeholder CDI bean required by {@link ExecutionSandbox} injection.
 * In production, extend this to wire in audit logging or metrics.
 */
@ApplicationScoped
public class SandboxInterceptor {
    // Intentionally minimal — execution logic is in ExecutionSandbox
    public void onEnter(String label) {}
    public void onCommit(int changes) {}
    public void onDiscard() {}
}
