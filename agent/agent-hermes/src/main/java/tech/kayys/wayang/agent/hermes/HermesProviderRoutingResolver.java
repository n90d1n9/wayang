package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Resolves provider/model routing hints without binding Hermes to a vendor.
 */
public final class HermesProviderRoutingResolver {

    private static final List<String> PROVIDER_KEYS = List.of(
            "hermes.provider",
            "hermes.llm.provider",
            "llm.provider",
            "model.provider",
            "modelProvider",
            "preferredProvider",
            "provider");

    private static final List<String> MODEL_KEYS = List.of(
            "hermes.model",
            "hermes.llm.model",
            "llm.model",
            "model.id",
            "modelId",
            "model");

    private static final List<String> LOCAL_KEYS = List.of(
            "hermes.preferLocalProvider",
            "preferLocalProvider",
            HermesMetadataKeys.PARAM_PREFER_LOCAL_PROVIDERS,
            "preferLocal",
            "localProvider",
            "local");

    private static final List<String> HIGH_CONTEXT_KEYS = List.of(
            "hermes.highContextRequired",
            "highContextRequired",
            "highContext",
            "longContext",
            "largeContext");

    private static final List<String> API_GATEWAY_KEYS = List.of(
            "hermes.apiGatewayPreferred",
            "apiGatewayPreferred",
            "apiGateway",
            "routeViaGateway",
            "gatewayProvider");

    private static final List<String> TOOL_CALLING_KEYS = List.of(
            "hermes.requireToolCalling",
            HermesMetadataKeys.PARAM_REQUIRE_TOOL_CALLING,
            "toolCalling",
            "toolsRequired");

    private final HermesAgentModeConfig config;

    public HermesProviderRoutingResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesProviderRoutingPlan resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<String> requestedProvider = values.firstText(PROVIDER_KEYS);
        Optional<String> requestedModel = requestModel(request, values);
        boolean localPreferred = values.firstBoolean(LOCAL_KEYS, "provider routing")
                .orElseGet(() -> config.preferLocalProviders() || HermesPromptSignals.suggestsLocalProvider(prompt));
        boolean highContextRequired = values.firstBoolean(HIGH_CONTEXT_KEYS, "provider routing")
                .orElseGet(() -> HermesPromptSignals.suggestsHighContext(prompt));
        boolean apiGatewayPreferred = values.firstBoolean(API_GATEWAY_KEYS, "provider routing")
                .orElseGet(() -> HermesPromptSignals.suggestsApiGateway(prompt));
        boolean toolCallingRequired = values.firstBoolean(TOOL_CALLING_KEYS, "provider routing")
                .orElse(config.requireToolCalling());
        String fallbackProvider = canonicalProvider(config.fallbackProvider());

        if (requestedProvider.isPresent()) {
            String provider = canonicalProvider(requestedProvider.orElseThrow());
            return new HermesProviderRoutingPlan(
                    provider,
                    provider,
                    fallbackProvider,
                    requestedModel.orElse(""),
                    requestedModel.orElse(""),
                    localPreferred || isLocalProvider(provider),
                    highContextRequired,
                    apiGatewayPreferred || isApiGatewayProvider(provider),
                    toolCallingRequired,
                    "explicit",
                    "explicit provider requested");
        }

        String configuredProvider = canonicalProvider(config.preferredProvider());
        if (!"auto".equals(configuredProvider)) {
            return new HermesProviderRoutingPlan(
                    configuredProvider,
                    "",
                    fallbackProvider,
                    requestedModel.orElse(""),
                    requestedModel.orElse(""),
                    localPreferred || isLocalProvider(configuredProvider),
                    highContextRequired,
                    apiGatewayPreferred || isApiGatewayProvider(configuredProvider),
                    toolCallingRequired,
                    "config",
                    "configured provider selected");
        }

        Optional<String> promptedProvider = HermesPromptSignals.provider(prompt);
        if (promptedProvider.isPresent()) {
            String provider = promptedProvider.orElseThrow();
            return new HermesProviderRoutingPlan(
                    provider,
                    "",
                    fallbackProvider,
                    requestedModel.orElse(""),
                    requestedModel.orElse(""),
                    localPreferred || isLocalProvider(provider),
                    highContextRequired,
                    apiGatewayPreferred || isApiGatewayProvider(provider),
                    toolCallingRequired,
                    "prompt",
                    "provider preference inferred from prompt");
        }

        if (localPreferred) {
            return new HermesProviderRoutingPlan(
                    "local",
                    "",
                    fallbackProvider,
                    requestedModel.orElse(""),
                    requestedModel.orElse(""),
                    true,
                    highContextRequired,
                    apiGatewayPreferred,
                    toolCallingRequired,
                    config.preferLocalProviders() ? "config" : "prompt",
                    "local provider preference selected");
        }

        if (apiGatewayPreferred) {
            return new HermesProviderRoutingPlan(
                    "api-gateway",
                    "",
                    fallbackProvider,
                    requestedModel.orElse(""),
                    requestedModel.orElse(""),
                    false,
                    highContextRequired,
                    true,
                    toolCallingRequired,
                    "prompt",
                    "API gateway preference inferred from request");
        }

        return new HermesProviderRoutingPlan(
                "auto",
                "",
                fallbackProvider,
                requestedModel.orElse(""),
                requestedModel.orElse(""),
                false,
                highContextRequired,
                false,
                toolCallingRequired,
                requestedModel.isPresent() ? "explicit" : "none",
                requestedModel.isPresent()
                        ? "explicit model requested; provider selection delegated to runtime"
                        : "provider selection delegated to runtime");
    }

    public HermesProviderRoutingPlan defaultPlan() {
        return resolve(null);
    }

    private static Optional<String> requestModel(AgentRequest request, HermesRequestValues values) {
        if (request != null && request.modelId() != null && !request.modelId().isBlank()) {
            return Optional.of(request.modelId().trim());
        }
        return values.firstText(MODEL_KEYS);
    }

    private static boolean isLocalProvider(String provider) {
        return switch (HermesRequestValues.normalize(provider)) {
            case "local", "ollama", "vllm", "llamacpp", "llama.cpp" -> true;
            default -> false;
        };
    }

    private static boolean isApiGatewayProvider(String provider) {
        return switch (HermesRequestValues.normalize(provider)) {
            case "openrouter", "nousportal", "api", "apigateway", "gateway" -> true;
            default -> false;
        };
    }

    private static String canonicalProvider(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "", "auto" -> "auto";
            case "local" -> "local";
            case "ollama" -> "ollama";
            case "vllm" -> "vllm";
            case "openrouter" -> "openrouter";
            case "nous", "nousportal" -> "nous-portal";
            case "api", "apigateway", "gateway" -> "api-gateway";
            default -> value == null ? "auto" : value.trim().toLowerCase(Locale.ROOT);
        };
    }
}
