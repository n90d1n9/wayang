package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class WayangA2aSpecAlignmentRequirements {

    private WayangA2aSpecAlignmentRequirements() {
    }

    static List<WayangA2aSpecAlignmentRequirement> from(A2aHttpRouteCatalog routeCatalog) {
        A2aHttpRouteCatalog resolvedCatalog = routeCatalog == null
                ? A2aHttpRouteCatalog.standard()
                : routeCatalog;
        List<WayangA2aSpecAlignmentRequirement> requirements = new ArrayList<>();
        requirements.add(protocolMetadataRequirement());
        requirements.add(bindingMetadataRequirement());
        requirements.addAll(WayangA2aAgentCardSpecAlignment.requirements());
        requirements.addAll(WayangA2aSpecAlignmentRouteRequirements.from(resolvedCatalog));
        requirements.addAll(WayangA2aSpecAlignmentJsonRpcRequirements.requirements());
        return List.copyOf(requirements);
    }

    private static WayangA2aSpecAlignmentRequirement protocolMetadataRequirement() {
        Map<String, Object> expected = Map.of(
                "protocolVersion", "1.0",
                "mediaType", "application/a2a+json",
                "eventStreamMediaType", "text/event-stream",
                "wellKnownAgentCardPath", "/.well-known/agent-card.json",
                "versionHeader", "A2A-Version",
                "extensionsHeader", "A2A-Extensions");
        Map<String, Object> actual = Map.of(
                "protocolVersion", A2aProtocol.VERSION,
                "mediaType", A2aProtocol.MEDIA_TYPE,
                "eventStreamMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE,
                "wellKnownAgentCardPath", A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH,
                "versionHeader", A2aProtocol.HEADER_VERSION,
                "extensionsHeader", A2aProtocol.HEADER_EXTENSIONS);
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "protocol.metadata",
                "protocol",
                "A2A v1.0 protocol metadata",
                expected,
                actual,
                "A2A protocol constants do not match the pinned v1.0 snapshot.");
    }

    private static WayangA2aSpecAlignmentRequirement bindingMetadataRequirement() {
        Map<String, Object> expected = Map.of(
                "bindings", List.of("JSONRPC", "GRPC", "HTTP+JSON"),
                "jsonRpcRequestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "jsonRpcResponseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "streamingResponseMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        Map<String, Object> actual = Map.of(
                "bindings", List.of(
                        A2aProtocol.BINDING_JSONRPC,
                        A2aProtocol.BINDING_GRPC,
                        A2aProtocol.BINDING_HTTP_JSON),
                "jsonRpcRequestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "jsonRpcResponseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                "streamingResponseMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "binding.metadata",
                "binding",
                "A2A transport binding metadata",
                expected,
                actual,
                "A2A binding constants do not match the pinned v1.0 snapshot.");
    }

}
