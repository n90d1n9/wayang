package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered projections for A2UI specification-alignment reports.
 */
final class WayangA2uiSpecAlignmentProjection {

    private WayangA2uiSpecAlignmentProjection() {
    }

    static Map<String, Object> report(WayangA2uiSpecAlignmentReport report) {
        WayangA2uiSpecAlignmentReport resolved = Objects.requireNonNull(report, "report");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", WayangA2uiSpecAlignmentReport.STANDARD_ID);
        values.put("specVersion", A2uiProtocol.VERSION);
        values.put("extensionUri", A2uiProtocol.EXTENSION_URI);
        values.put("standard", standardDescriptor());
        values.put("aligned", resolved.aligned());
        values.put("requirementCount", resolved.requirementCount());
        values.put("alignedCount", resolved.alignedCount());
        values.put("gapCount", resolved.gapCount());
        values.put("requirementIds", resolved.requirementIds());
        values.put("gapIds", resolved.gapIds());
        values.put("routeCatalog", resolved.routeCatalog().toMap());
        values.put("requirements", resolved.requirements().stream()
                .map(WayangA2uiSpecAlignmentProjection::requirement)
                .toList());
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> standardDescriptor() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", WayangA2uiSpecAlignmentReport.STANDARD_ID);
        values.put("name", WayangA2uiSpecAlignmentReport.STANDARD_NAME);
        values.put("version", A2uiProtocol.VERSION);
        values.put("binding", WayangA2uiSpecAlignmentReport.BINDING_HTTP);
        values.put("specUrl", A2uiProtocol.STANDARD_CATALOG_ID);
        values.put("extensionUri", A2uiProtocol.EXTENSION_URI);
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> requirement(WayangA2uiSpecAlignmentRequirement requirement) {
        WayangA2uiSpecAlignmentRequirement resolved = Objects.requireNonNull(requirement, "requirement");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", resolved.id());
        values.put("category", resolved.category());
        values.put("title", resolved.title());
        values.put("aligned", resolved.aligned());
        values.put("expected", resolved.expected());
        values.put("actual", resolved.actual());
        if (!resolved.message().isBlank()) {
            values.put("message", resolved.message());
        }
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> routeExpectation(WayangA2uiHttpRoute route) {
        WayangA2uiHttpRoute resolved = Objects.requireNonNull(route, "route");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("present", true);
        values.put("operation", resolved.operation());
        values.put("method", resolved.method());
        values.put("pathSuffix", routeSuffix(resolved.path()));
        values.put("requestContentType", resolved.requestContentType());
        values.put("responseContentType", resolved.responseContentType());
        values.put("requestBodyRequired", resolved.requestBodyRequired());
        values.put("allowsOptions", true);
        return WayangA2uiTransportMaps.freeze(values);
    }

    static Map<String, Object> routeActual(
            WayangA2uiHttpRoute expectedRoute,
            WayangA2uiHttpRoute actualRoute) {
        WayangA2uiHttpRoute resolvedExpected = Objects.requireNonNull(expectedRoute, "expectedRoute");
        WayangA2uiHttpRoute resolvedActual = Objects.requireNonNull(actualRoute, "actualRoute");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("present", true);
        values.put("operation", resolvedActual.operation());
        values.put("method", resolvedActual.method());
        values.put("path", resolvedActual.path());
        values.put("pathSuffix", routeSuffix(resolvedExpected.path()));
        values.put("pathSuffixMatched", pathSuffixMatches(resolvedExpected.path(), resolvedActual.path()));
        values.put("requestContentType", resolvedActual.requestContentType());
        values.put("responseContentType", resolvedActual.responseContentType());
        values.put("requestBodyRequired", resolvedActual.requestBodyRequired());
        values.put("allowsOptions", resolvedActual.allowedMethods().contains("OPTIONS"));
        return WayangA2uiTransportMaps.freeze(values);
    }

    static boolean pathSuffixMatches(String expectedPath, String actualPath) {
        String suffix = routeSuffix(expectedPath);
        String actual = WayangA2uiDecodeValues.text(actualPath);
        String expected = WayangA2uiDecodeValues.text(expectedPath);
        return actual.equals(expected) || actual.endsWith(suffix);
    }

    private static String routeSuffix(String path) {
        String normalized = WayangA2uiDecodeValues.text(path);
        if (normalized.startsWith(WayangA2uiHttpRoute.PATH_ROOT + "/")) {
            return normalized.substring(WayangA2uiHttpRoute.PATH_ROOT.length());
        }
        return normalized;
    }
}
