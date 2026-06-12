package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.support.StringMaps;
import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;
import tech.kayys.wayang.a2ui.wayang.support.RecordMaps;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;
import tech.kayys.wayang.a2ui.wayang.support.ProjectionCollections;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Classifies current source-compatible A2UI classes into their target module
 * boundaries before and during physical package moves.
 */
final class WayangA2uiBoundaryPlacement {

    private static final List<String> SUPPORT_FIRST_CANDIDATES = List.of(
            "DecodeCollections",
            "DecodeValues",
            "ProjectionCollections",
            "RecordCollections",
            "RecordMaps",
            "RecordNumbers",
            "RecordValues",
            "StringMaps");
    private static final List<String> TRANSPORT_FIRST_CANDIDATES = List.of(
            "TransportMetadataProjection",
            "TransportProjection");
    private static final List<String> ACTION_FIRST_CANDIDATES = List.of(
            "ActionBindingReportProjection",
            "ActionContextReader",
            "ActionGate",
            "ActionMetadata",
            "ActionQueries",
            "ActionResponses");
    private static final List<String> HTTP_FIRST_CANDIDATES = List.of(
            "HttpActionBindingProbeProjection",
            "HttpBindingReportProjection",
            "HttpBindingReportProbeProjection",
            "HttpEndpointDiagnosticPlanProjection",
            "HttpEndpointDiagnosticProjection",
            "HttpEndpointProjection",
            "HttpExpectationProjection",
            "HttpOperationalDiagnosticsProjection",
            "HttpPublicationProjection",
            "HttpReadinessProbeProjection",
            "HttpRouteProjection",
            "HttpScenarioProjection",
            "HttpSmokeProbeProjection",
            "HttpSmokeResultProjection");
    private static final List<String> PROJECTION_LOCAL_CANDIDATES = List.of("ProjectionMaps");

    private static final List<BoundaryRule> RULES = List.of(
            new BoundaryRule(WayangA2uiModuleBoundary.SUPPORT, SUPPORT_FIRST_CANDIDATES::contains),
            new BoundaryRule(WayangA2uiModuleBoundary.TRANSPORT, TRANSPORT_FIRST_CANDIDATES::contains),
            new BoundaryRule(WayangA2uiModuleBoundary.ACTION, ACTION_FIRST_CANDIDATES::contains),
            new BoundaryRule(WayangA2uiModuleBoundary.HTTP, HTTP_FIRST_CANDIDATES::contains),
            new BoundaryRule(WayangA2uiModuleBoundary.PROJECTION, PROJECTION_LOCAL_CANDIDATES::contains),
            new BoundaryRule(WayangA2uiModuleBoundary.PROJECTION, name -> name.endsWith("Projection")),
            new BoundaryRule(WayangA2uiModuleBoundary.HTTP, name -> name.contains("Http")),
            new BoundaryRule(WayangA2uiModuleBoundary.SURFACE,
                    name -> name.contains("Surface") || name.startsWith("Run")),
            new BoundaryRule(WayangA2uiModuleBoundary.ACTION, name -> name.contains("Action")),
            new BoundaryRule(WayangA2uiModuleBoundary.SPEC,
                    name -> name.contains("SpecAlignment") || name.contains("StandardAlignment")),
            new BoundaryRule(WayangA2uiModuleBoundary.SESSION, name -> name.contains("Session")),
            new BoundaryRule(WayangA2uiModuleBoundary.BRIDGE, name -> name.contains("Bridge")),
            new BoundaryRule(WayangA2uiModuleBoundary.TRANSPORT, name -> name.contains("Transport")),
            new BoundaryRule(WayangA2uiModuleBoundary.FACADE, name -> name.equals("WayangA2ui")
                    || name.equals("WayangA2uiModuleBoundary")
                    || name.equals("WayangA2uiBoundaryPlacement")));

    private WayangA2uiBoundaryPlacement() {
    }

    static WayangA2uiModuleBoundary classify(Class<?> type) {
        Class<?> resolved = Objects.requireNonNull(type, "type");
        return WayangA2uiModuleBoundary.targetSubpackages().stream()
                .filter(boundary -> boundary.packageName().equals(resolved.getPackageName()))
                .findFirst()
                .orElseGet(() -> classifySimpleName(resolved.getSimpleName()));
    }

    static WayangA2uiModuleBoundary classifySimpleName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            throw new IllegalArgumentException("A2UI class name must not be blank");
        }
        return RULES.stream()
                .filter(rule -> rule.matches(simpleName))
                .map(BoundaryRule::boundary)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No A2UI module boundary placement for " + simpleName));
    }

    static List<String> supportFirstCandidates() {
        return SUPPORT_FIRST_CANDIDATES;
    }

    private record BoundaryRule(
            WayangA2uiModuleBoundary boundary,
            Predicate<String> matcher) {

        private boolean matches(String simpleName) {
            return matcher.test(simpleName);
        }
    }
}
