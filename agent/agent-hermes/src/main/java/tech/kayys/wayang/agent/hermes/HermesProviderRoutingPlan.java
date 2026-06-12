package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advisory LLM provider/model routing contract for Hermes requests.
 */
public record HermesProviderRoutingPlan(
        String selectedProvider,
        String requestedProvider,
        String fallbackProvider,
        String model,
        String requestedModel,
        boolean localPreferred,
        boolean highContextRequired,
        boolean apiGatewayPreferred,
        boolean toolCallingRequired,
        String source,
        String reason) {

    public HermesProviderRoutingPlan {
        selectedProvider = HermesText.trimOr(selectedProvider, "auto");
        requestedProvider = HermesText.trimOr(requestedProvider, "");
        fallbackProvider = HermesText.trimOr(fallbackProvider, "auto");
        model = HermesText.trimOr(model, "");
        requestedModel = HermesText.trimOr(requestedModel, "");
        source = HermesText.trimOr(source, "none");
        reason = HermesText.trimOr(reason, "provider selection delegated to runtime");
    }

    public boolean explicitProvider() {
        return !requestedProvider.isBlank();
    }

    public boolean explicitModel() {
        return !requestedModel.isBlank();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("selectedProvider", selectedProvider);
        metadata.put("requestedProvider", requestedProvider);
        metadata.put("fallbackProvider", fallbackProvider);
        metadata.put("model", model);
        metadata.put("requestedModel", requestedModel);
        metadata.put("localPreferred", localPreferred);
        metadata.put("highContextRequired", highContextRequired);
        metadata.put("apiGatewayPreferred", apiGatewayPreferred);
        metadata.put("toolCallingRequired", toolCallingRequired);
        metadata.put("source", source);
        metadata.put("reason", reason);
        metadata.put("explicitProvider", explicitProvider());
        metadata.put("explicitModel", explicitModel());
        return Map.copyOf(metadata);
    }
}
