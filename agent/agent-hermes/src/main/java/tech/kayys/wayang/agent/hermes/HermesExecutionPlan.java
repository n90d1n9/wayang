package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Advisory execution backend choice for a Hermes request.
 */
public record HermesExecutionPlan(
        String backend,
        String requestedBackend,
        boolean explicitRequest,
        boolean isolationRequired,
        boolean remotePreferred,
        boolean serverlessPreferred,
        List<String> availableBackends,
        String reason) {

    public HermesExecutionPlan {
        backend = HermesText.trimOr(backend, "none");
        requestedBackend = requestedBackend == null ? "" : requestedBackend.trim();
        availableBackends = HermesText.trimmedList(availableBackends);
        reason = HermesText.trimOr(reason, "default backend selected");
    }

    public boolean executable() {
        return !"none".equalsIgnoreCase(backend);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("backend", backend);
        metadata.put("requestedBackend", requestedBackend);
        metadata.put("explicitRequest", explicitRequest);
        metadata.put("isolationRequired", isolationRequired);
        metadata.put("remotePreferred", remotePreferred);
        metadata.put("serverlessPreferred", serverlessPreferred);
        metadata.put("availableBackends", availableBackends);
        metadata.put("reason", reason);
        metadata.put("executable", executable());
        return Map.copyOf(metadata);
    }
}
