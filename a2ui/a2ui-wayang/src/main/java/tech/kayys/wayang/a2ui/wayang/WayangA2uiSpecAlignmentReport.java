package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.a2ui.wayang.projection.SpecAlignmentProjection;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;

import tech.kayys.wayang.a2ui.core.A2uiBeginRendering;
import tech.kayys.wayang.a2ui.core.A2uiClientError;
import tech.kayys.wayang.a2ui.core.A2uiDataModelUpdate;
import tech.kayys.wayang.a2ui.core.A2uiDataPart;
import tech.kayys.wayang.a2ui.core.A2uiDeleteSurface;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.core.A2uiSurfaceUpdate;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Machine-readable alignment snapshot for Wayang's pinned A2UI v0.8 surface.
 */
public record WayangA2uiSpecAlignmentReport(
        WayangA2uiHttpRouteCatalog routeCatalog,
        List<WayangA2uiSpecAlignmentRequirement> requirements) {

    public static final String STANDARD_ID = "a2ui";
    public static final String STANDARD_NAME = "Agent-to-User Interface";
    public static final String BINDING_HTTP = "HTTP";

    private static final List<String> SERVER_MESSAGE_KEYS = List.of(
            "dataModelUpdate",
            "surfaceUpdate",
            "beginRendering",
            "deleteSurface");
    private static final List<String> CLIENT_MESSAGE_KEYS = List.of("userAction", "error");

    public WayangA2uiSpecAlignmentReport {
        routeCatalog = routeCatalog == null ? WayangA2uiHttpRouteCatalog.defaultCatalog() : routeCatalog;
        requirements = requirements == null
                ? List.of()
                : requirements.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static WayangA2uiSpecAlignmentReport defaultReport() {
        return from(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiSpecAlignmentReport defaults() {
        return defaultReport();
    }

    public static WayangA2uiSpecAlignmentReport from(WayangA2uiHttpRouteCatalog routeCatalog) {
        WayangA2uiHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? WayangA2uiHttpRouteCatalog.defaultCatalog()
                : routeCatalog;
        return new WayangA2uiSpecAlignmentReport(
                resolvedCatalog,
                List.of(
                        protocolMetadataRequirement(),
                        transportContentRequirement(),
                        serverMessageRequirement(),
                        clientMessageRequirement(),
                        dataPartRequirement(),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.exchange()),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.surfaceCatalog()),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.routeCatalog()),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.bindingReport()),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.smoke()),
                        routeRequirement(resolvedCatalog, WayangA2uiHttpRoute.readiness())));
    }

    public boolean aligned() {
        return gapCount() == 0;
    }

    public int requirementCount() {
        return requirements.size();
    }

    public int alignedCount() {
        return (int) requirements.stream()
                .filter(WayangA2uiSpecAlignmentRequirement::aligned)
                .count();
    }

    public int gapCount() {
        return requirementCount() - alignedCount();
    }

    public List<WayangA2uiSpecAlignmentRequirement> gaps() {
        return requirements.stream()
                .filter(requirement -> !requirement.aligned())
                .toList();
    }

    public List<String> requirementIds() {
        return requirements.stream()
                .map(WayangA2uiSpecAlignmentRequirement::id)
                .toList();
    }

    public List<String> gapIds() {
        return gaps().stream()
                .map(WayangA2uiSpecAlignmentRequirement::id)
                .toList();
    }

    public Map<String, Object> toMap() {
        return SpecAlignmentProjection.report(this);
    }

    static Map<String, Object> standardDescriptor() {
        return SpecAlignmentProjection.standardDescriptor();
    }

    private static WayangA2uiSpecAlignmentRequirement protocolMetadataRequirement() {
        Map<String, Object> expected = Map.of(
                "specVersion", "v0.8",
                "extensionUri", "https://a2ui.org/a2a-extension/a2ui/v0.8",
                "mimeType", "application/json+a2ui",
                "standardCatalogId", "https://a2ui.org/specification/v0_8/standard_catalog_definition.json",
                "clientCapabilitiesKey", "a2uiClientCapabilities");
        Map<String, Object> actual = Map.of(
                "specVersion", A2uiProtocol.VERSION,
                "extensionUri", A2uiProtocol.EXTENSION_URI,
                "mimeType", A2uiProtocol.MIME_TYPE,
                "standardCatalogId", A2uiProtocol.STANDARD_CATALOG_ID,
                "clientCapabilitiesKey", A2uiProtocol.CLIENT_CAPABILITIES_KEY);
        return requirement(
                "protocol.metadata",
                "protocol",
                "A2UI v0.8 protocol metadata",
                expected.equals(actual),
                expected,
                actual,
                "A2UI protocol constants do not match the pinned v0.8 snapshot.");
    }

    private static WayangA2uiSpecAlignmentRequirement transportContentRequirement() {
        Map<String, Object> expected = Map.of(
                "a2uiMimeType", A2uiProtocol.MIME_TYPE,
                "jsonMimeType", "application/json",
                "problemJsonMimeType", "application/problem+json",
                "jsonlEncoding", "jsonl",
                "jsonEncoding", "json");
        Map<String, Object> actual = Map.of(
                "a2uiMimeType", WayangA2uiTransportContent.MIME_A2UI,
                "jsonMimeType", WayangA2uiTransportContent.MIME_JSON,
                "problemJsonMimeType", WayangA2uiTransportContent.MIME_PROBLEM_JSON,
                "jsonlEncoding", WayangA2uiTransportContent.ENCODING_JSONL,
                "jsonEncoding", WayangA2uiTransportContent.ENCODING_JSON);
        return requirement(
                "transport.content",
                "transport",
                "A2UI transport content metadata",
                expected.equals(actual),
                expected,
                actual,
                "A2UI transport content constants do not match the pinned v0.8 snapshot.");
    }

    private static WayangA2uiSpecAlignmentRequirement serverMessageRequirement() {
        List<String> actualKeys = List.of(
                messageKey(A2uiDataModelUpdate.root("main")),
                messageKey(A2uiSurfaceUpdate.of("main")),
                messageKey(A2uiBeginRendering.standard("main", "root")),
                messageKey(new A2uiDeleteSurface("main")));
        return messageKeyRequirement(
                "message.server_keys",
                "A2UI server message keys",
                SERVER_MESSAGE_KEYS,
                actualKeys);
    }

    private static WayangA2uiSpecAlignmentRequirement clientMessageRequirement() {
        List<String> actualKeys = List.of(
                messageKey(new A2uiUserAction(
                        "wayang.run.inspect",
                        "main",
                        "button",
                        Instant.parse("2026-05-31T00:00:00Z"),
                        Map.of("runId", "run-1"))),
                messageKey(new A2uiClientError("main", "Unable to render", Map.of("code", "render_failed"))));
        return messageKeyRequirement(
                "message.client_keys",
                "A2UI client message keys",
                CLIENT_MESSAGE_KEYS,
                actualKeys);
    }

    private static WayangA2uiSpecAlignmentRequirement dataPartRequirement() {
        Map<String, Object> dataPart = A2uiDataPart.of(A2uiBeginRendering.standard("main", "root")).toPayload();
        Map<String, Object> metadata = map(dataPart.get("metadata"));
        Map<String, Object> expected = Map.of(
                "kind", "data",
                "mimeType", A2uiProtocol.MIME_TYPE);
        Map<String, Object> actual = Map.of(
                "kind", DecodeValues.text(dataPart.get("kind")),
                "mimeType", DecodeValues.text(metadata.get("mimeType")));
        return requirement(
                "transport.data_part",
                "transport",
                "A2A DataPart wrapper metadata",
                expected.equals(actual),
                expected,
                actual,
                "A2UI DataPart wrapper metadata does not match the pinned v0.8 snapshot.");
    }

    private static WayangA2uiSpecAlignmentRequirement routeRequirement(
            WayangA2uiHttpRouteCatalog catalog,
            WayangA2uiHttpRoute expectedRoute) {
        return catalog.routeForOperation(expectedRoute.operation())
                .map(actualRoute -> routeRequirement(expectedRoute, actualRoute))
                .orElseGet(() -> WayangA2uiSpecAlignmentRequirement.gap(
                        routeRequirementId(expectedRoute.operation()),
                        "route",
                        "A2UI HTTP route " + expectedRoute.operation(),
                        SpecAlignmentProjection.routeExpectation(expectedRoute),
                        Map.of("present", false),
                        "A2UI HTTP route is missing from the local route catalog."));
    }

    private static WayangA2uiSpecAlignmentRequirement routeRequirement(
            WayangA2uiHttpRoute expectedRoute,
            WayangA2uiHttpRoute actualRoute) {
        Map<String, Object> expected = SpecAlignmentProjection.routeExpectation(expectedRoute);
        Map<String, Object> actual = SpecAlignmentProjection.routeActual(expectedRoute, actualRoute);
        boolean aligned = expectedRoute.method().equals(actualRoute.method())
                && SpecAlignmentProjection.pathSuffixMatches(expectedRoute.path(), actualRoute.path())
                && expectedRoute.requestContentType().equals(actualRoute.requestContentType())
                && expectedRoute.responseContentType().equals(actualRoute.responseContentType())
                && expectedRoute.requestBodyRequired() == actualRoute.requestBodyRequired()
                && actualRoute.allowedMethods().contains("OPTIONS");
        return requirement(
                routeRequirementId(expectedRoute.operation()),
                "route",
                "A2UI HTTP route " + expectedRoute.operation(),
                aligned,
                expected,
                actual,
                "A2UI HTTP route shape does not match the pinned v0.8 snapshot.");
    }

    private static WayangA2uiSpecAlignmentRequirement messageKeyRequirement(
            String id,
            String title,
            List<String> expectedKeys,
            List<String> actualKeys) {
        Map<String, Object> expected = Map.of("messageKeys", expectedKeys);
        Map<String, Object> actual = Map.of("messageKeys", actualKeys);
        return requirement(
                id,
                "message",
                title,
                expected.equals(actual),
                expected,
                actual,
                "A2UI message keys do not match the pinned v0.8 snapshot.");
    }

    private static WayangA2uiSpecAlignmentRequirement requirement(
            String id,
            String category,
            String title,
            boolean aligned,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        if (aligned) {
            return WayangA2uiSpecAlignmentRequirement.aligned(id, category, title, expected, actual);
        }
        return WayangA2uiSpecAlignmentRequirement.gap(id, category, title, expected, actual, message);
    }

    private static String routeRequirementId(String operation) {
        return "route." + operation;
    }

    private static String messageKey(tech.kayys.wayang.a2ui.core.A2uiServerMessage message) {
        return message.toPayload().keySet().stream().findFirst().orElse("");
    }

    private static String messageKey(tech.kayys.wayang.a2ui.core.A2uiClientMessage message) {
        return message.toPayload().keySet().stream().findFirst().orElse("");
    }

    private static Map<String, Object> map(Object value) {
        return TransportMaps.copyMap(value);
    }
}
