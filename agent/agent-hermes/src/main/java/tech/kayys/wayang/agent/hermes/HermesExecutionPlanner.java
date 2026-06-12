package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;

/**
 * Selects a configured execution backend for Hermes work.
 */
public final class HermesExecutionPlanner {

    private static final List<String> EXPLICIT_BACKEND_KEYS = List.of(
            "hermes.executionBackend",
            HermesMetadataKeys.PARAM_EXECUTION_BACKEND,
            "execution.backend",
            "execution_backend",
            "backend");

    private static final List<String> ISOLATION_KEYS = List.of(
            "hermes.isolationRequired",
            "isolationRequired",
            "execution.isolation",
            "sandbox");

    private static final List<String> REMOTE_KEYS = List.of(
            "hermes.remotePreferred",
            "remotePreferred",
            "execution.remote");

    private static final List<String> SERVERLESS_KEYS = List.of(
            "hermes.serverlessPreferred",
            "serverlessPreferred",
            "execution.serverless");

    private final HermesAgentModeConfig config;

    public HermesExecutionPlanner(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesExecutionPlan plan(AgentRequest request) {
        List<String> available = config.executionBackends();
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<String> requested = values.firstText(EXPLICIT_BACKEND_KEYS);
        boolean isolationRequired = values.firstBoolean(ISOLATION_KEYS, "execution")
                .orElseGet(() -> HermesPromptSignals.suggestsIsolation(prompt));
        boolean remotePreferred = values.firstBoolean(REMOTE_KEYS, "execution")
                .orElseGet(() -> HermesPromptSignals.suggestsRemote(prompt));
        boolean serverlessPreferred = values.firstBoolean(SERVERLESS_KEYS, "execution")
                .orElseGet(() -> HermesPromptSignals.suggestsServerless(prompt));

        if (requested.isPresent()) {
            String requestedBackend = requested.orElseThrow();
            Optional<String> selected = configuredBackend(available, requestedBackend);
            if (selected.isPresent()) {
                return new HermesExecutionPlan(
                        selected.orElseThrow(),
                        requestedBackend,
                        true,
                        isolationRequired,
                        remotePreferred,
                        serverlessPreferred,
                        available,
                        "explicit backend requested");
            }
            return new HermesExecutionPlan(
                    fallback(available),
                    requestedBackend,
                    true,
                    isolationRequired,
                    remotePreferred,
                    serverlessPreferred,
                    available,
                    "requested backend unavailable; selected fallback");
        }

        if (serverlessPreferred) {
            return selectedPlan(available, List.of("modal", "daytona", "docker", "ssh", "local"),
                    isolationRequired, remotePreferred, true, "serverless execution preferred");
        }
        if (remotePreferred) {
            return selectedPlan(available, List.of("ssh", "daytona", "modal", "docker", "local"),
                    isolationRequired, true, false, "remote execution preferred");
        }
        if (isolationRequired) {
            return selectedPlan(available, List.of("docker", "singularity", "daytona", "modal", "ssh", "local"),
                    true, false, false, "isolated execution preferred");
        }
        if (config.preferLocalProviders()) {
            return selectedPlan(available, List.of("local", "docker", "ssh"),
                    false, false, false, "local execution preferred");
        }
        return new HermesExecutionPlan(
                fallback(available),
                "",
                false,
                false,
                false,
                false,
                available,
                available.isEmpty() ? "no execution backends configured" : "default backend selected");
    }

    public HermesExecutionPlan defaultPlan() {
        return plan(null);
    }

    private static HermesExecutionPlan selectedPlan(
            List<String> available,
            List<String> preferredOrder,
            boolean isolationRequired,
            boolean remotePreferred,
            boolean serverlessPreferred,
            String reason) {
        String selected = preferredOrder.stream()
                .map(preferred -> configuredBackend(available, preferred))
                .filter(Optional::isPresent)
                .map(Optional::orElseThrow)
                .findFirst()
                .orElseGet(() -> fallback(available));
        return new HermesExecutionPlan(
                selected,
                "",
                false,
                isolationRequired,
                remotePreferred,
                serverlessPreferred,
                available,
                selected.equals("none") ? "no execution backends configured" : reason);
    }

    private static Optional<String> configuredBackend(List<String> available, String requested) {
        String normalized = HermesRequestValues.normalize(requested);
        return available.stream()
                .filter(value -> HermesRequestValues.normalize(value).equals(normalized))
                .findFirst();
    }

    private static String fallback(List<String> available) {
        return available == null || available.isEmpty() ? "none" : available.getFirst();
    }
}
