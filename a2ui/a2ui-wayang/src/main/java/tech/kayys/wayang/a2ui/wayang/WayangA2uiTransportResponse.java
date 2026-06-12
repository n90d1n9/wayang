package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Transport-neutral outbound A2UI response.
 */
public record WayangA2uiTransportResponse(
        String mimeType,
        String bodyEncoding,
        String body,
        List<Map<String, Object>> dataParts,
        long handledCount,
        long rejectedCount,
        Map<String, Object> metadata) {

    public WayangA2uiTransportResponse(
            String mimeType,
            String bodyEncoding,
            String body,
            List<Map<String, Object>> dataParts,
            long handledCount,
            long rejectedCount) {
        this(mimeType, bodyEncoding, body, dataParts, handledCount, rejectedCount, Map.of());
    }

    public WayangA2uiTransportResponse {
        mimeType = mimeType == null || mimeType.isBlank() ? WayangA2uiTransportContent.MIME_A2UI : mimeType.trim();
        bodyEncoding = bodyEncoding == null || bodyEncoding.isBlank()
                ? WayangA2uiTransportContent.ENCODING_JSONL
                : bodyEncoding.trim();
        body = body == null ? "" : body;
        dataParts = TransportMaps.copyMaps(dataParts);
        metadata = TransportMaps.copy(metadata);
    }

    public static WayangA2uiTransportResponse from(WayangA2uiSessionResult result) {
        WayangA2uiSessionResult resolved = Objects.requireNonNull(result, "result");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_A2UI,
                WayangA2uiTransportContent.ENCODING_JSONL,
                resolved.responseJsonl(),
                resolved.responseDataParts(),
                resolved.handledCount(),
                resolved.rejectedCount(),
                WayangA2uiTransportMetadata.sessionResult(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiSurfaceCatalog catalog) {
        WayangA2uiSurfaceCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                0,
                0,
                WayangA2uiTransportMetadata.surfaceCatalog(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiActionBindingReport report) {
        WayangA2uiActionBindingReport resolved = Objects.requireNonNull(report, "report");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                resolved.complete() ? 1 : 0,
                resolved.complete() ? 0 : 1,
                WayangA2uiTransportMetadata.actionBindingReport(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = Objects.requireNonNull(catalog, "catalog");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                0,
                0,
                WayangA2uiTransportMetadata.httpRouteCatalog(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiHttpBindingReport report) {
        WayangA2uiHttpBindingReport resolved = Objects.requireNonNull(report, "report");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                0,
                0,
                WayangA2uiTransportMetadata.httpBindingReport(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiHttpSmokeResult result) {
        WayangA2uiHttpSmokeResult resolved = Objects.requireNonNull(result, "result");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                resolved.passed() ? 1 : 0,
                resolved.passed() ? 0 : 1,
                WayangA2uiTransportMetadata.httpSmokeResult(resolved));
    }

    public static WayangA2uiTransportResponse from(WayangA2uiHttpReadinessProbeResult result) {
        WayangA2uiHttpReadinessProbeResult resolved = Objects.requireNonNull(result, "result");
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                resolved.passed() ? 1 : 0,
                resolved.passed() ? 0 : 1,
                WayangA2uiTransportMetadata.httpReadinessProbe(resolved));
    }

    public static WayangA2uiTransportResponse error(String code, String message) {
        return error(WayangA2uiTransportError.of(code, message));
    }

    public static WayangA2uiTransportResponse error(WayangA2uiTransportError error) {
        WayangA2uiTransportError resolved = error == null
                ? WayangA2uiTransportError.defaultError()
                : error;
        return new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_PROBLEM_JSON,
                WayangA2uiTransportContent.ENCODING_JSON,
                json(resolved.toMap()),
                List.of(),
                0,
                1,
                WayangA2uiTransportMetadata.error(resolved));
    }

    public static WayangA2uiTransportResponse fromMap(Map<?, ?> values) {
        return WayangA2uiTransportResponseDecoder.fromMap(values);
    }

    public static WayangA2uiTransportResponse fromJson(String json) {
        return WayangA2uiTransportResponseDecoder.fromJson(json);
    }

    public boolean empty() {
        return body.isBlank() && dataParts.isEmpty();
    }

    public WayangA2uiTransportOutcome outcome() {
        if (transportError().isPresent()) {
            return WayangA2uiTransportOutcome.TRANSPORT_ERROR;
        }
        if (handledCount > 0 && rejectedCount > 0) {
            return WayangA2uiTransportOutcome.PARTIAL_SUCCESS;
        }
        if (rejectedCount > 0) {
            return WayangA2uiTransportOutcome.ACTION_REJECTED;
        }
        if (empty()) {
            return WayangA2uiTransportOutcome.EMPTY;
        }
        return WayangA2uiTransportOutcome.SUCCESS;
    }

    public Map<String, Object> toMap() {
        return WayangA2uiTransportEnvelope.response(
                mimeType,
                bodyEncoding,
                body,
                dataParts,
                handledCount,
                rejectedCount,
                metadata,
                outcome(),
                empty());
    }

    public String toJson() {
        return json(toMap());
    }

    public Optional<WayangA2uiTransportError> transportError() {
        Object error = metadata.get(WayangA2uiTransportFields.ERROR);
        if (error instanceof Map<?, ?> map) {
            return Optional.of(WayangA2uiTransportError.fromMap(map));
        }
        if (WayangA2uiTransportFields.RESPONSE_KIND_TRANSPORT_ERROR.equals(
                metadata.get(WayangA2uiTransportFields.RESPONSE_KIND))) {
            Object code = metadata.get(WayangA2uiTransportFields.ERROR_CODE);
            return Optional.of(WayangA2uiTransportError.of(code == null ? "" : String.valueOf(code), ""));
        }
        return Optional.empty();
    }

    public WayangA2uiTransportResponse withMetadata(Map<String, ?> extraMetadata) {
        if (extraMetadata == null || extraMetadata.isEmpty()) {
            return this;
        }
        return new WayangA2uiTransportResponse(
                mimeType,
                bodyEncoding,
                body,
                dataParts,
                handledCount,
                rejectedCount,
                WayangA2uiTransportMetadata.merge(metadata, extraMetadata));
    }

    private static String json(Map<String, Object> payload) {
        return TransportJson.json(payload, "Unable to encode A2UI transport response");
    }
}
