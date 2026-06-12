package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpTool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class McpToolLifecycleCounts {

    private final Map<String, Integer> lifecycleStates = emptyLifecycleStates();
    private int total;
    private int enabled;
    private int disabled;
    private int stale;
    private int active;
    private int serverDisabled;
    private int retired;

    void add(McpTool tool) {
        if (tool == null) {
            return;
        }
        total++;
        String lifecycleState = McpToolLifecycle.lifecycleState(tool);
        lifecycleStates.compute(lifecycleState, (key, count) -> count == null ? 1 : count + 1);
        if (McpToolLifecycle.LIFECYCLE_ACTIVE.equals(lifecycleState)) {
            active++;
        }
        if (tool.isEnabled()) {
            enabled++;
        } else {
            disabled++;
        }
        if (McpToolLifecycle.isStale(tool)) {
            stale++;
        }
        if (McpToolLifecycle.isServerDisabled(tool)) {
            serverDisabled++;
        }
        if (McpToolLifecycle.isRetired(tool)) {
            retired++;
        }
    }

    int total() {
        return total;
    }

    int enabled() {
        return enabled;
    }

    int disabled() {
        return disabled;
    }

    int stale() {
        return stale;
    }

    int active() {
        return active;
    }

    int serverDisabled() {
        return serverDisabled;
    }

    int retired() {
        return retired;
    }

    Map<String, Integer> lifecycleStates() {
        return copyLifecycleStates(lifecycleStates);
    }

    static Map<String, Integer> emptyLifecycleStates() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(McpToolLifecycle.LIFECYCLE_ACTIVE, 0);
        counts.put(McpToolLifecycle.LIFECYCLE_DISABLED, 0);
        counts.put(McpToolLifecycle.LIFECYCLE_SERVER_DISABLED, 0);
        counts.put(McpToolLifecycle.LIFECYCLE_STALE, 0);
        counts.put(McpToolLifecycle.LIFECYCLE_RETIRED, 0);
        return counts;
    }

    static Map<String, Integer> copyLifecycleStates(Map<String, Integer> counts) {
        Map<String, Integer> copy = emptyLifecycleStates();
        if (counts != null && !counts.isEmpty()) {
            copy.putAll(counts);
        }
        return Collections.unmodifiableMap(copy);
    }
}
