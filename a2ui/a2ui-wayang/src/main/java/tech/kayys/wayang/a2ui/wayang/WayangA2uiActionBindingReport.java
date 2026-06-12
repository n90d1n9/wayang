package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.action.ActionBindingReportProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import tech.kayys.wayang.a2ui.wayang.support.ProjectionCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;

/**
 * Coverage report between an A2UI action policy and action handler registry.
 */
public record WayangA2uiActionBindingReport(
        List<String> policyActions,
        List<String> handlerActions,
        List<String> missingHandlerActions,
        List<String> orphanHandlerActions) {

    public WayangA2uiActionBindingReport {
        policyActions = normalize(policyActions);
        handlerActions = normalize(handlerActions);
        missingHandlerActions = normalize(missingHandlerActions);
        orphanHandlerActions = normalize(orphanHandlerActions);
    }

    public static WayangA2uiActionBindingReport of(
            WayangA2uiActionPolicy policy,
            WayangA2uiActionHandlers handlers) {
        WayangA2uiActionPolicy resolvedPolicy = policy == null
                ? WayangA2uiActionPolicy.defaultPolicy()
                : policy;
        List<String> policyActions = orderedPolicyActions(resolvedPolicy);
        List<String> registeredHandlerActions = handlers == null ? null : handlers.actionNames();
        List<String> handlerActions = orderedHandlerActions(policyActions, registeredHandlerActions);
        List<String> missingHandlers = policyActions.stream()
                .filter(actionName -> !handlerActions.contains(actionName))
                .toList();
        List<String> orphanHandlers = handlerActions.stream()
                .filter(actionName -> !policyActions.contains(actionName))
                .toList();
        return new WayangA2uiActionBindingReport(
                policyActions,
                handlerActions,
                missingHandlers,
                orphanHandlers);
    }

    public static WayangA2uiActionBindingReport from(WayangA2uiHttpResponse response) {
        return WayangA2uiActionBindingReportDecoder.from(response);
    }

    public static WayangA2uiActionBindingReport from(WayangA2uiTransportResponse response) {
        return WayangA2uiActionBindingReportDecoder.from(response);
    }

    public static WayangA2uiActionBindingReport fromMap(Map<?, ?> values) {
        return WayangA2uiActionBindingReportDecoder.fromMap(values);
    }

    public static WayangA2uiActionBindingReport fromJson(String json) {
        return WayangA2uiActionBindingReportDecoder.fromJson(json);
    }

    public int policyActionCount() {
        return policyActions.size();
    }

    public int handlerActionCount() {
        return handlerActions.size();
    }

    public boolean complete() {
        return missingHandlerActions.isEmpty() && orphanHandlerActions.isEmpty();
    }

    public Map<String, Object> toMap() {
        return ActionBindingReportProjection.report(this);
    }

    public String toJson() {
        return TransportJson.json(toMap(), "Unable to encode A2UI action binding report");
    }

    private static List<String> orderedPolicyActions(WayangA2uiActionPolicy policy) {
        return ProjectionCollections.referenceOrderThenSortedRemainder(
                WayangA2uiActions.runLifecycleActionOrder(),
                normalize(policy.allowedActions().stream().toList()));
    }

    private static List<String> orderedHandlerActions(
            List<String> policyActions,
            List<String> registeredHandlerActions) {
        return ProjectionCollections.referenceOrderThenSortedRemainder(
                policyActions,
                registeredHandlerActions);
    }

    private static List<String> normalize(List<String> values) {
        return DecodeCollections.distinctNonBlankTexts(values);
    }
}
