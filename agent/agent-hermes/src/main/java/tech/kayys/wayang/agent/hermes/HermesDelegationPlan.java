package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Advisory sub-agent fan-out plan for Hermes workstreams.
 */
public record HermesDelegationPlan(
        boolean delegationEnabled,
        boolean requested,
        boolean delegated,
        int suggestedSubAgents,
        int maxSubAgents,
        List<String> lanes,
        String isolationMode,
        String source,
        String reason) {

    public HermesDelegationPlan {
        suggestedSubAgents = Math.max(0, suggestedSubAgents);
        maxSubAgents = Math.max(1, maxSubAgents);
        lanes = HermesText.distinctTrimmedList(lanes);
        isolationMode = HermesText.trimOr(isolationMode, delegated ? "context-isolated" : "none");
        source = HermesText.trimOr(source, "none");
        reason = HermesText.trimOr(reason, "no delegation requested");
    }

    public boolean active() {
        return delegationEnabled && delegated && suggestedSubAgents > 1;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("delegationEnabled", delegationEnabled);
        metadata.put("requested", requested);
        metadata.put("delegated", delegated);
        metadata.put("active", active());
        metadata.put("suggestedSubAgents", suggestedSubAgents);
        metadata.put("maxSubAgents", maxSubAgents);
        metadata.put("lanes", lanes);
        metadata.put("isolationMode", isolationMode);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }
}
