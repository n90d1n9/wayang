package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimeEventQuery;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalDirective;

import java.util.function.BiFunction;

/**
 * Maps named Hermes journal presets to runtime directives.
 */
final class HermesJournalPresetMapper {

    HermesRuntimeJournalDirective latest(HermesJournalPresetRequest request) {
        return HermesRuntimeJournalDirective.latest(limit(request));
    }

    HermesRuntimeJournalDirective failures(HermesJournalPresetRequest request) {
        return HermesRuntimeJournalDirective.inspect(HermesRuntimeEventQuery.failures(limit(request)));
    }

    HermesRuntimeJournalDirective learning(HermesJournalPresetRequest request) {
        return HermesRuntimeJournalDirective.inspect(HermesRuntimeEventQuery.learning(limit(request)));
    }

    HermesRuntimeJournalDirective request(String requestId, HermesJournalPresetRequest request) {
        return identity("requestId", requestId, request, HermesRuntimeEventQuery::forRequest);
    }

    HermesRuntimeJournalDirective session(String sessionId, HermesJournalPresetRequest request) {
        return identity("sessionId", sessionId, request, HermesRuntimeEventQuery::forSession);
    }

    HermesRuntimeJournalDirective user(String userId, HermesJournalPresetRequest request) {
        return identity("userId", userId, request, HermesRuntimeEventQuery::forUser);
    }

    HermesRuntimeJournalDirective tenant(String tenantId, HermesJournalPresetRequest request) {
        return identity("tenantId", tenantId, request, HermesRuntimeEventQuery::forTenant);
    }

    private int limit(HermesJournalPresetRequest request) {
        return request == null ? 0 : request.limit();
    }

    private HermesRuntimeJournalDirective identity(
            String parameterName,
            String value,
            HermesJournalPresetRequest request,
            BiFunction<String, Integer, HermesRuntimeEventQuery> queryFactory) {
        return HermesRuntimeJournalDirective.inspect(queryFactory.apply(required(parameterName, value), limit(request)));
    }

    private String required(String parameterName, String value) {
        String resolved = value == null ? "" : value.trim();
        if (resolved.isBlank()) {
            throw new IllegalArgumentException(parameterName + " is required");
        }
        return resolved;
    }
}
