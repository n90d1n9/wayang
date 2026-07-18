package tech.kayys.gamelan.governance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.hitl.HumanInTheLoop;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tool execution permissions for the agent.
 * Acts as an approval interceptor before a tool call is dispatched.
 */
@ApplicationScoped
public class PermissionManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionManager.class);

    @Inject
    HumanInTheLoop hitl;

    // In-memory record of persistently approved tool signatures for the session
    private final Set<String> alwaysApprovedSignatures = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Checks if the tool call is permitted. If it requires approval and is not
     * already pre-approved, it delegates to HumanInTheLoop.
     *
     * @param sessionId the current session
     * @param call the tool call to intercept
     * @param description a human-readable description of the call
     * @return true if permitted to execute, false if denied
     */
    public boolean isPermitted(String sessionId, ToolCall call, String description) {
        // Simple signature for exact match (e.g., toolName + params)
        String signature = call.name() + ":" + description;

        if (alwaysApprovedSignatures.contains(signature)) {
            log.debug("Tool call automatically permitted by previous approval: {}", signature);
            return true;
        }

        if (hitl.hasGate(call.name())) {
            boolean approved = hitl.requestApproval(sessionId, call.name(), description);
            if (approved) {
                return true;
            } else {
                return false;
            }
        }

        return true; // No gate, permitted by default
    }

    /**
     * Persistently approve a specific tool call signature for this session.
     */
    public void grantAlwaysApprove(String signature) {
        alwaysApprovedSignatures.add(signature);
    }
}
