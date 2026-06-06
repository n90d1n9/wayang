package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Coverage report between an A2UI HTTP route catalog and operation dispatcher.
 */
public record WayangA2uiHttpBindingReport(
        List<String> routeOperations,
        List<String> handlerOperations,
        List<String> missingHandlerOperations,
        List<String> orphanHandlerOperations) {

    public WayangA2uiHttpBindingReport {
        routeOperations = normalize(routeOperations);
        handlerOperations = normalize(handlerOperations);
        missingHandlerOperations = normalize(missingHandlerOperations);
        orphanHandlerOperations = normalize(orphanHandlerOperations);
    }

    public static WayangA2uiHttpBindingReport of(
            WayangA2uiHttpRouteCatalog routeCatalog,
            WayangA2uiHttpOperationDispatcher dispatcher) {
        WayangA2uiHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? WayangA2uiHttpRouteCatalog.defaultCatalog()
                : routeCatalog;
        List<String> routeOperations = resolvedCatalog.routes().stream()
                .map(WayangA2uiHttpRoute::operation)
                .distinct()
                .toList();
        List<String> registeredHandlerOperations = dispatcher == null ? List.of() : dispatcher.operations();
        List<String> handlerOperations = orderedHandlerOperations(routeOperations, registeredHandlerOperations);
        List<String> missingHandlers = routeOperations.stream()
                .filter(operation -> !handlerOperations.contains(operation))
                .toList();
        List<String> orphanHandlers = handlerOperations.stream()
                .filter(operation -> !routeOperations.contains(operation))
                .toList();
        return new WayangA2uiHttpBindingReport(
                routeOperations,
                handlerOperations,
                missingHandlers,
                orphanHandlers);
    }

    public int routeOperationCount() {
        return routeOperations.size();
    }

    public int handlerOperationCount() {
        return handlerOperations.size();
    }

    public boolean complete() {
        return missingHandlerOperations.isEmpty() && orphanHandlerOperations.isEmpty();
    }

    public Map<String, Object> toMap() {
        return WayangA2uiHttpBindingReportProjection.report(this);
    }

    private static List<String> normalize(List<String> values) {
        return WayangA2uiDecodeCollections.distinctNonBlankTexts(values);
    }

    private static List<String> orderedHandlerOperations(
            List<String> routeOperations,
            List<String> registeredHandlerOperations) {
        if (registeredHandlerOperations == null || registeredHandlerOperations.isEmpty()) {
            return List.of();
        }
        List<String> ordered = new ArrayList<>();
        routeOperations.stream()
                .filter(registeredHandlerOperations::contains)
                .forEach(ordered::add);
        registeredHandlerOperations.stream()
                .filter(operation -> !routeOperations.contains(operation))
                .sorted()
                .forEach(ordered::add);
        return List.copyOf(ordered);
    }
}
