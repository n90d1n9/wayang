package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiDataPart;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiProtocol;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiTransportAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void exchangesJsonlPayloadsThroughSession() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.runLifecycle());

        WayangA2uiTransportResponse response = adapter.exchangeJsonl(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1")))
                        + "\n"
                        + codec.line(action(WayangA2uiActions.RUN_WAIT, Map.of("runId", "run-1"))));

        assertThat(response.mimeType()).isEqualTo(A2uiProtocol.MIME_TYPE);
        assertThat(response.bodyEncoding()).isEqualTo("jsonl");
        assertThat(response.handledCount()).isEqualTo(2);
        assertThat(response.rejectedCount()).isZero();
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS);
        assertThat(response.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSONL.name())
                .containsEntry("responseKind", "a2ui-session-result")
                .containsEntry("actionCount", 2)
                .containsEntry("handledCount", 2L)
                .containsEntry("rejectedCount", 0L)
                .containsEntry("messageCount", 6)
                .containsEntry("dataPartCount", 6);
        assertThat(response.body())
                .contains("RUNNING")
                .contains("COMPLETED");
        assertThat(response.dataParts()).hasSameSizeAs(response.body().strip().split("\\R"));
        assertThat(response.empty()).isFalse();
        assertThat(sdk.inspected).isEqualTo(1);
        assertThat(sdk.waited).isEqualTo(1);
    }

    @Test
    void classifiesPartialActionResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        WayangA2uiTransportResponse response = adapter.exchangeJsonl(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1")))
                        + "\n"
                        + codec.line(action(WayangA2uiActions.RUN_CANCEL, Map.of("runId", "run-1"))));

        assertThat(response.handledCount()).isEqualTo(1L);
        assertThat(response.rejectedCount()).isEqualTo(1L);
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.PARTIAL_SUCCESS);
        assertThat(response.toMap()).containsEntry("outcome", WayangA2uiTransportOutcome.PARTIAL_SUCCESS.name());
        assertThat(response.body())
                .contains("wayang.run.inspect")
                .contains("A2UI action is not allowed.");
        assertThat(sdk.inspected).isEqualTo(1);
        assertThat(sdk.cancelled).isZero();
    }

    @Test
    void exchangesDataPartMapsThroughSession() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);
        A2uiUserAction action = action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"));

        WayangA2uiTransportResponse response = adapter.exchangeDataPart(A2uiDataPart.of(action).toPayload());

        assertThat(response.handledCount()).isEqualTo(1);
        assertThat(response.rejectedCount()).isZero();
        assertThat(response.body()).contains("wayang.run.inspect");
        assertThat(response.dataParts()).hasSize(3);
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsAndExportsTransportRequestsAsMapAndJsonEnvelopes() throws Exception {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "userAction");
        dataPart.put("nested", nested);
        dataPart.put("ignored", null);

        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.dataPart(dataPart);
        Map<String, Object> envelope = request.toMap();
        JsonNode jsonEnvelope = objectMapper.readTree(request.toJson());
        WayangA2uiTransportRequest fromMap = WayangA2uiTransportRequest.fromMap(Map.of(
                "kind", "dataPartMap",
                "dataPart", dataPart));
        WayangA2uiTransportRequest fromJson = WayangA2uiTransportRequest.fromJson(request.toJson());
        nested.put("value", "changed");

        assertThat(envelope)
                .containsEntry("kind", WayangA2uiTransportPayloadKind.DATA_PART_MAP.name())
                .containsEntry("body", "");
        assertThat((Map<String, Object>) envelope.get("dataPart"))
                .containsEntry("kind", "userAction")
                .doesNotContainKey("ignored");
        assertThat(jsonEnvelope.at("/kind").asText()).isEqualTo(WayangA2uiTransportPayloadKind.DATA_PART_MAP.name());
        assertThat(jsonEnvelope.at("/dataPart/nested/value").asText()).isEqualTo("original");
        assertThat(fromMap.kind()).isEqualTo(WayangA2uiTransportPayloadKind.DATA_PART_MAP);
        assertThat((Map<String, Object>) fromMap.dataPart().get("nested"))
                .containsEntry("value", "original");
        assertThat(fromJson).isEqualTo(request);
    }

    @Test
    void exposesCanonicalTransportEnvelopeFieldNames() {
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.surfaceCatalog();
        WayangA2uiTransportResponse response = WayangA2uiTransportResponse.error("bad_request", "Bad request.");

        assertThat(request.toMap())
                .containsKeys(
                        WayangA2uiTransportFields.KIND,
                        WayangA2uiTransportFields.BODY,
                        WayangA2uiTransportFields.DATA_PART);
        assertThat(response.toMap())
                .containsKeys(
                        WayangA2uiTransportFields.MIME_TYPE,
                        WayangA2uiTransportFields.BODY_ENCODING,
                        WayangA2uiTransportFields.BODY,
                        WayangA2uiTransportFields.DATA_PARTS,
                        WayangA2uiTransportFields.HANDLED_COUNT,
                        WayangA2uiTransportFields.REJECTED_COUNT,
                        WayangA2uiTransportFields.METADATA,
                        WayangA2uiTransportFields.OUTCOME,
                        WayangA2uiTransportFields.EMPTY);
        assertThat(response.metadata())
                .containsKeys(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.ERROR_CODE,
                        WayangA2uiTransportFields.ERROR);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsCanonicalTransportEnvelopes() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "userAction");
        dataPart.put("nested", nested);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("nested", nested);

        Map<String, Object> requestEnvelope = WayangA2uiTransportEnvelope.request(
                WayangA2uiTransportPayloadKind.JSON_LINE,
                null,
                dataPart);
        Map<String, Object> responseEnvelope = WayangA2uiTransportEnvelope.response(
                WayangA2uiTransportContent.MIME_A2UI,
                WayangA2uiTransportContent.ENCODING_JSONL,
                null,
                List.of(dataPart),
                1,
                0,
                metadata,
                WayangA2uiTransportOutcome.SUCCESS,
                false);
        nested.put("value", "changed");

        assertThat(requestEnvelope.keySet())
                .containsExactly(
                        WayangA2uiTransportFields.KIND,
                        WayangA2uiTransportFields.BODY,
                        WayangA2uiTransportFields.DATA_PART);
        assertThat(requestEnvelope)
                .containsEntry(WayangA2uiTransportFields.KIND, WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry(WayangA2uiTransportFields.BODY, "");
        assertThat((Map<String, Object>) ((Map<String, Object>) requestEnvelope.get(
                WayangA2uiTransportFields.DATA_PART)).get("nested"))
                .containsEntry("value", "original");
        assertThat(responseEnvelope.keySet())
                .containsExactly(
                        WayangA2uiTransportFields.MIME_TYPE,
                        WayangA2uiTransportFields.BODY_ENCODING,
                        WayangA2uiTransportFields.BODY,
                        WayangA2uiTransportFields.DATA_PARTS,
                        WayangA2uiTransportFields.HANDLED_COUNT,
                        WayangA2uiTransportFields.REJECTED_COUNT,
                        WayangA2uiTransportFields.METADATA,
                        WayangA2uiTransportFields.OUTCOME,
                        WayangA2uiTransportFields.EMPTY);
        assertThat(responseEnvelope)
                .containsEntry(WayangA2uiTransportFields.BODY, "")
                .containsEntry(WayangA2uiTransportFields.OUTCOME, WayangA2uiTransportOutcome.SUCCESS.name())
                .containsEntry(WayangA2uiTransportFields.EMPTY, false);
        assertThat((Map<String, Object>) ((Map<String, Object>) responseEnvelope.get(
                WayangA2uiTransportFields.METADATA)).get("nested"))
                .containsEntry("value", "original");
        assertThatThrownBy(() -> requestEnvelope.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> responseEnvelope.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void decodesCanonicalTransportRequests() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<Object, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "userAction");
        dataPart.put("nested", nested);
        dataPart.put("ignored", null);

        WayangA2uiTransportRequest request = WayangA2uiTransportRequestDecoder.fromMap(Map.of(
                WayangA2uiTransportFields.KIND, "dataPartMap",
                WayangA2uiTransportFields.BODY, " 42 ",
                WayangA2uiTransportFields.DATA_PART, dataPart));
        nested.put("value", "changed");

        assertThat(WayangA2uiTransportRequestDecoder.payloadKind(WayangA2uiTransportPayloadKind.JSONL))
                .isEqualTo(WayangA2uiTransportPayloadKind.JSONL);
        assertThat(WayangA2uiTransportRequestDecoder.payloadKind("json-line"))
                .isEqualTo(WayangA2uiTransportPayloadKind.JSON_LINE);
        assertThat(WayangA2uiTransportRequestDecoder.payloadKind("data.part.json"))
                .isEqualTo(WayangA2uiTransportPayloadKind.DATA_PART_JSON);
        assertThat(WayangA2uiTransportRequestDecoder.payloadKind("surface catalog"))
                .isEqualTo(WayangA2uiTransportPayloadKind.SURFACE_CATALOG);
        assertThat(WayangA2uiTransportRequestDecoder.payloadKind("actionBindingReport"))
                .isEqualTo(WayangA2uiTransportPayloadKind.ACTION_BINDING_REPORT);
        assertThat(request.kind()).isEqualTo(WayangA2uiTransportPayloadKind.DATA_PART_MAP);
        assertThat(request.body()).isEqualTo(" 42 ");
        assertThat(request.dataPart())
                .containsEntry("kind", "userAction")
                .doesNotContainKey("ignored");
        assertThat((Map<String, Object>) request.dataPart().get("nested"))
                .containsEntry("value", "original");
        assertThatThrownBy(() -> WayangA2uiTransportRequestDecoder.payloadKind(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport request kind must not be blank");
        assertThatThrownBy(() -> WayangA2uiTransportRequestDecoder.payloadKind("unknown-kind"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported A2UI transport request kind: unknown-kind");
    }

    @Test
    void exposesCanonicalTransportContentTypesAndEncodings() {
        WayangA2uiTransportResponse defaultResponse = new WayangA2uiTransportResponse(
                "",
                "",
                "",
                List.of(),
                0,
                0);
        WayangA2uiTransportResponse catalogResponse = WayangA2uiTransportResponse.from(
                WayangA2uiSurfaceCatalog.from(null));
        WayangA2uiTransportResponse errorResponse = WayangA2uiTransportResponse.error("bad_request", "Bad request.");

        assertThat(WayangA2uiTransportContent.MIME_A2UI).isEqualTo(A2uiProtocol.MIME_TYPE);
        assertThat(defaultResponse.mimeType()).isEqualTo(WayangA2uiTransportContent.MIME_A2UI);
        assertThat(defaultResponse.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSONL);
        assertThat(catalogResponse.mimeType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(catalogResponse.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(errorResponse.mimeType()).isEqualTo(WayangA2uiTransportContent.MIME_PROBLEM_JSON);
        assertThat(errorResponse.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsCanonicalTransportMetadata() {
        WayangA2uiActionResult result = WayangA2uiActionResult.handled(
                WayangA2uiActions.RUN_INSPECT,
                "run-1",
                "Run inspected.",
                List.of(),
                Map.of());
        WayangA2uiSessionResult sessionResult = WayangA2uiSessionResult.of(List.of(result), codec);
        WayangA2uiSurfaceCatalog catalog = WayangA2uiSurfaceCatalog.from(null);
        WayangA2uiActionBindingReport actionBindingReport = new WayangA2uiActionBindingReport(
                List.of(WayangA2uiActions.RUN_INSPECT),
                List.of(WayangA2uiActions.RUN_INSPECT),
                List.of(),
                List.of());
        WayangA2uiTransportError error = WayangA2uiTransportError.of("bad_request", "Bad request.");
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> extraMetadata = new LinkedHashMap<>();
        extraMetadata.put("nested", nested);
        extraMetadata.put("ignored", null);

        Map<String, Object> requestMetadata =
                WayangA2uiTransportMetadata.request(WayangA2uiTransportPayloadKind.JSON_LINE);
        Map<String, Object> sessionMetadata = WayangA2uiTransportMetadata.sessionResult(sessionResult);
        Map<String, Object> catalogMetadata = WayangA2uiTransportMetadata.surfaceCatalog(catalog);
        Map<String, Object> actionBindingMetadata =
                WayangA2uiTransportMetadata.actionBindingReport(actionBindingReport);
        Map<String, Object> errorMetadata = WayangA2uiTransportMetadata.error(error);
        Map<String, Object> mergedMetadata = WayangA2uiTransportMetadata.merge(
                Map.of("base", "value"),
                extraMetadata);
        nested.put("value", "changed");

        assertThat(requestMetadata)
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND, WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(sessionMetadata)
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SESSION_RESULT)
                .containsEntry(WayangA2uiTransportFields.ACTION_COUNT, 1)
                .containsEntry(WayangA2uiTransportFields.HANDLED_COUNT, 1L)
                .containsEntry(WayangA2uiTransportFields.REJECTED_COUNT, 0L)
                .containsEntry(WayangA2uiTransportFields.EMPTY, false);
        assertThat((Integer) sessionMetadata.get(WayangA2uiTransportFields.MESSAGE_COUNT)).isPositive();
        assertThat((Integer) sessionMetadata.get(WayangA2uiTransportFields.DATA_PART_COUNT)).isPositive();
        assertThat(catalogMetadata)
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SURFACE_CATALOG)
                .containsEntry(WayangA2uiTransportFields.SURFACE_KIND_COUNT, catalog.surfaceKinds().size())
                .containsEntry(WayangA2uiTransportFields.DESCRIPTOR_COUNT, catalog.descriptorCount());
        assertThat(actionBindingMetadata)
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)
                .containsEntry(WayangA2uiTransportFields.COMPLETE, true)
                .containsEntry(WayangA2uiTransportFields.POLICY_ACTION_COUNT, 1)
                .containsEntry(WayangA2uiTransportFields.HANDLER_ACTION_COUNT, 1)
                .containsEntry(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, 0)
                .containsEntry(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, 0);
        assertThat(errorMetadata)
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_TRANSPORT_ERROR)
                .containsEntry(WayangA2uiTransportFields.ERROR_CODE, "bad_request")
                .containsEntry(WayangA2uiTransportFields.HANDLED_COUNT, 0L)
                .containsEntry(WayangA2uiTransportFields.REJECTED_COUNT, 1L);
        assertThat((Map<String, Object>) errorMetadata.get(WayangA2uiTransportFields.ERROR))
                .containsEntry(WayangA2uiTransportFields.CODE, "bad_request")
                .containsEntry(WayangA2uiTransportFields.MESSAGE, "Bad request.");
        assertThat(mergedMetadata)
                .containsEntry("base", "value")
                .doesNotContainKey("ignored");
        assertThat((Map<String, Object>) mergedMetadata.get("nested"))
                .containsEntry("value", "original");
    }

    @Test
    void usesSharedTransportJsonFailureMessages() {
        assertThatThrownBy(() -> WayangA2uiTransportRequest.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport request JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiTransportRequest.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI transport request JSON");
        assertThatThrownBy(() -> WayangA2uiTransportResponse.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport response JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiTransportResponse.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI transport response JSON");
    }

    @Test
    void exchangesRequestsDecodedFromJsonEnvelopes() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"))));

        WayangA2uiTransportResponse response = adapter.exchange(
                WayangA2uiTransportRequest.fromJson(request.toJson()));

        assertThat(response.handledCount()).isEqualTo(1);
        assertThat(response.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(response.body()).contains("wayang.run.inspect");
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exchangesMapEnvelopesAsMapResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.jsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"))));

        Map<String, Object> response = adapter.exchangeEnvelopeAsMap(request.toMap());

        assertThat(response)
                .containsEntry("mimeType", A2uiProtocol.MIME_TYPE)
                .containsEntry("bodyEncoding", "jsonl")
                .containsEntry("handledCount", 1L)
                .containsEntry("rejectedCount", 0L);
        assertThat((Map<String, Object>) response.get("metadata"))
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry("responseKind", "a2ui-session-result");
        assertThat((String) response.get("body")).contains("wayang.run.inspect");
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void exchangesJsonEnvelopesAsJsonResponses() throws Exception {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        String response = adapter.exchangeEnvelopeAsJson(WayangA2uiTransportRequest.surfaceCatalog().toJson());
        JsonNode responseEnvelope = objectMapper.readTree(response);

        assertThat(responseEnvelope.at("/bodyEncoding").asText()).isEqualTo("json");
        assertThat(responseEnvelope.at("/metadata/requestKind").asText())
                .isEqualTo(WayangA2uiTransportPayloadKind.SURFACE_CATALOG.name());
        assertThat(responseEnvelope.at("/metadata/responseKind").asText()).isEqualTo("surface-catalog");
        assertThat(responseEnvelope.at("/handledCount").asLong()).isZero();
        assertThat(responseEnvelope.at("/dataParts").size()).isZero();
        assertThat(responseEnvelope.at("/body").asText()).contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void exchangesActionBindingReportThroughTransport() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.runLifecycle());

        WayangA2uiTransportResponse response = adapter.exchangeActionBindingReport();
        WayangA2uiActionBindingReport report = WayangA2uiActionBindingReport.from(response);

        assertThat(response.mimeType()).isEqualTo(WayangA2uiTransportContent.MIME_JSON);
        assertThat(response.bodyEncoding()).isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(response.handledCount()).isEqualTo(1L);
        assertThat(response.rejectedCount()).isZero();
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS);
        assertThat(response.metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND,
                        WayangA2uiTransportPayloadKind.ACTION_BINDING_REPORT.name())
                .containsEntry(WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)
                .containsEntry(WayangA2uiTransportFields.COMPLETE, true)
                .containsEntry(WayangA2uiTransportFields.POLICY_ACTION_COUNT, 5)
                .containsEntry(WayangA2uiTransportFields.HANDLER_ACTION_COUNT, 5);
        assertThat(report.complete()).isTrue();
        assertThat(report.policyActionCount()).isEqualTo(5);
        assertThat(report.handlerActionCount()).isEqualTo(5);
        assertThat(report.policyActions()).containsExactlyElementsOf(WayangA2uiActions.runLifecycleActionOrder());
        assertThat(report.handlerActions()).containsExactlyElementsOf(WayangA2uiActions.runLifecycleActionOrder());
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void exchangesInvalidMapEnvelopesAsErrorMapResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        Map<String, Object> response = adapter.exchangeEnvelopeAsMapOrError(Map.of("kind", "unknown-kind"));

        assertThat(response)
                .containsEntry("mimeType", "application/problem+json")
                .containsEntry("bodyEncoding", "json")
                .containsEntry("handledCount", 0L)
                .containsEntry("rejectedCount", 1L)
                .containsEntry("empty", false);
        Map<String, Object> metadata = (Map<String, Object>) response.get("metadata");
        assertThat(metadata)
                .containsEntry("responseKind", "transport-error")
                .containsEntry("errorCode", "invalid_request_envelope");
        assertThat((String) response.get("body"))
                .contains("invalid_request_envelope")
                .contains("Unsupported A2UI transport request kind");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void exchangesInvalidJsonEnvelopesAsErrorJsonResponses() throws Exception {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        String response = adapter.exchangeEnvelopeAsJsonOrError("{not-json");
        JsonNode responseEnvelope = objectMapper.readTree(response);

        assertThat(responseEnvelope.at("/mimeType").asText()).isEqualTo("application/problem+json");
        assertThat(responseEnvelope.at("/metadata/responseKind").asText()).isEqualTo("transport-error");
        assertThat(responseEnvelope.at("/metadata/errorCode").asText()).isEqualTo("invalid_request_json");
        assertThat(responseEnvelope.at("/body").asText()).contains("invalid_request_json");
        assertThat(responseEnvelope.at("/rejectedCount").asLong()).isEqualTo(1L);
        assertThat(sdk.inspected).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsTransportErrorResponses() throws Exception {
        WayangA2uiTransportResponse response = WayangA2uiTransportResponse.error("bad_request", "Bad request.");
        JsonNode body = objectMapper.readTree(response.body());
        WayangA2uiTransportError error = response.transportError().orElseThrow();
        Map<String, Object> metadataError = (Map<String, Object>) response.metadata().get("error");

        assertThat(response.mimeType()).isEqualTo("application/problem+json");
        assertThat(response.bodyEncoding()).isEqualTo("json");
        assertThat(response.handledCount()).isZero();
        assertThat(response.rejectedCount()).isEqualTo(1L);
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(response.metadata())
                .containsEntry("responseKind", "transport-error")
                .containsEntry("errorCode", "bad_request");
        assertThat(metadataError)
                .containsEntry("code", "bad_request")
                .containsEntry("message", "Bad request.");
        assertThat(error)
                .isEqualTo(WayangA2uiTransportError.of("bad_request", "Bad request."));
        assertThat(body.at("/code").asText()).isEqualTo("bad_request");
        assertThat(body.at("/message").asText()).isEqualTo("Bad request.");
    }

    @Test
    void normalizesTransportErrorDetails() {
        WayangA2uiTransportError error = WayangA2uiTransportError.fromMap(Map.of(
                "code", " custom_error ",
                "message", " Custom message. "));
        WayangA2uiTransportResponse response = WayangA2uiTransportResponse.error(null, "");

        assertThat(error.code()).isEqualTo("custom_error");
        assertThat(error.message()).isEqualTo("Custom message.");
        assertThat(response.transportError())
                .contains(WayangA2uiTransportError.of(
                        WayangA2uiTransportError.DEFAULT_CODE,
                        WayangA2uiTransportError.DEFAULT_MESSAGE));
    }

    @Test
    void classifiesRejectedAndEmptyTransportResponses() {
        WayangA2uiTransportResponse rejected = new WayangA2uiTransportResponse(
                A2uiProtocol.MIME_TYPE,
                "jsonl",
                "rejected",
                List.of(),
                0,
                1);
        WayangA2uiTransportResponse empty = new WayangA2uiTransportResponse(
                A2uiProtocol.MIME_TYPE,
                "jsonl",
                "",
                List.of(),
                0,
                0);

        assertThat(rejected.outcome()).isEqualTo(WayangA2uiTransportOutcome.ACTION_REJECTED);
        assertThat(empty.outcome()).isEqualTo(WayangA2uiTransportOutcome.EMPTY);
        assertThat(empty.toMap()).containsEntry("outcome", WayangA2uiTransportOutcome.EMPTY.name());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportsTransportResponsesAsMapAndJsonEnvelopes() throws Exception {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        WayangA2uiTransportResponse response = adapter.exchangeJsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"))));
        Map<String, Object> envelope = response.toMap();
        JsonNode jsonEnvelope = objectMapper.readTree(response.toJson());

        assertThat(envelope)
                .containsEntry("mimeType", A2uiProtocol.MIME_TYPE)
                .containsEntry("bodyEncoding", "jsonl")
                .containsEntry("body", response.body())
                .containsEntry("handledCount", 1L)
                .containsEntry("rejectedCount", 0L)
                .containsEntry("empty", false);
        assertThat((List<Map<String, Object>>) envelope.get("dataParts"))
                .hasSameSizeAs(response.dataParts());
        assertThat((Map<String, Object>) envelope.get("metadata"))
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry("responseKind", "a2ui-session-result");
        assertThat(jsonEnvelope.at("/mimeType").asText()).isEqualTo(A2uiProtocol.MIME_TYPE);
        assertThat(jsonEnvelope.at("/metadata/requestKind").asText())
                .isEqualTo(WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(jsonEnvelope.at("/dataParts").size()).isEqualTo(response.dataParts().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsTransportResponsesFromMapAndJsonEnvelopes() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        WayangA2uiTransportResponse response = adapter.exchangeJsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"))));
        WayangA2uiTransportResponse fromMap = WayangA2uiTransportResponse.fromMap(response.toMap());
        WayangA2uiTransportResponse fromJson = WayangA2uiTransportResponse.fromJson(response.toJson());

        assertThat(fromMap.mimeType()).isEqualTo(response.mimeType());
        assertThat(fromMap.bodyEncoding()).isEqualTo(response.bodyEncoding());
        assertThat(fromMap.body()).isEqualTo(response.body());
        assertThat(fromMap.handledCount()).isEqualTo(1L);
        assertThat(fromMap.rejectedCount()).isZero();
        assertThat(fromMap.dataParts()).hasSameSizeAs(response.dataParts());
        assertThat(fromMap.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry("responseKind", "a2ui-session-result");
        assertThat(fromJson.mimeType()).isEqualTo(response.mimeType());
        assertThat(fromJson.bodyEncoding()).isEqualTo(response.bodyEncoding());
        assertThat(fromJson.body()).isEqualTo(response.body());
        assertThat(fromJson.handledCount()).isEqualTo(1L);
        assertThat(fromJson.rejectedCount()).isZero();
        assertThat(fromJson.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS);
        assertThat(fromJson.dataParts()).hasSameSizeAs(response.dataParts());
        assertThat((Map<String, Object>) fromJson.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry("responseKind", "a2ui-session-result");
    }

    @Test
    void decodesTransportResponseCountsAndDataParts() {
        WayangA2uiTransportResponse response = WayangA2uiTransportResponseDecoder.fromMap(Map.of(
                WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON,
                WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON,
                WayangA2uiTransportFields.BODY, " {} ",
                WayangA2uiTransportFields.DATA_PARTS, Map.of("kind", "data"),
                WayangA2uiTransportFields.HANDLED_COUNT, "2",
                WayangA2uiTransportFields.REJECTED_COUNT, "1",
                WayangA2uiTransportFields.METADATA, Map.of("source", "fixture")));

        assertThat(response.handledCount()).isEqualTo(2L);
        assertThat(response.rejectedCount()).isEqualTo(1L);
        assertThat(response.body()).isEqualTo(" {} ");
        assertThat(response.dataParts()).hasSize(1);
        assertThat(response.metadata()).containsEntry("source", "fixture");
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.PARTIAL_SUCCESS);
        assertThatThrownBy(() -> WayangA2uiTransportResponseDecoder.fromMap(Map.of(
                WayangA2uiTransportFields.HANDLED_COUNT, "not-a-number")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI transport response count must be numeric: handledCount");
    }

    @Test
    void exchangesRejectedActionsAsRenderableTransportResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(sdk);

        WayangA2uiTransportResponse response = adapter.exchangeJsonLine(
                codec.line(action(WayangA2uiActions.RUN_CANCEL, Map.of("runId", "run-1"))));

        assertThat(response.handledCount()).isZero();
        assertThat(response.rejectedCount()).isEqualTo(1);
        assertThat(response.body())
                .contains("Action rejected")
                .contains("A2UI action is not allowed.");
        assertThat(response.dataParts()).hasSize(3);
        assertThat(sdk.cancelled).isZero();
    }

    @Test
    void canUseCustomSurfaceRegistryAtTransportBoundary() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.builder()
                .register(
                        "custom.inspection",
                        AgentRunInspection.class,
                        inspection -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.handled(
                                        "custom.inspect",
                                        inspection.runId(),
                                        "Custom inspection surface: " + inspection.runId(),
                                        List.of(),
                                        Map.of("known", inspection.known())),
                                9))
                .build();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.inspectOnly(),
                registry);

        WayangA2uiTransportResponse response = adapter.exchangeJsonLine(
                codec.line(action(WayangA2uiActions.RUN_INSPECT, Map.of("runId", "run-1"))));

        assertThat(response.handledCount()).isEqualTo(1);
        assertThat(response.body())
                .contains("wayang-action-result-9-custom-inspect")
                .contains("Custom inspection surface: run-1")
                .doesNotContain("wayang-run-run-1");
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void exposesSurfaceCatalogAtTransportBoundary() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.readOnly());

        WayangA2uiSurfaceCatalog catalog = adapter.surfaceCatalog();

        assertThat(catalog.surfaceKinds())
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS, WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(catalog.descriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .extracting(WayangA2uiSurfaceDescriptor::modelType)
                .containsExactly(WayangA2uiActionFeedback.class, WayangA2uiActionResult.class);
        assertThat(catalog.toMap())
                .containsEntry("descriptorCount", catalog.descriptorCount());
    }

    @Test
    void exchangesSurfaceCatalogRequestsAsJsonResponses() throws Exception {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.readOnly());
        WayangA2uiTransportRequest request = WayangA2uiTransportRequest.surfaceCatalog();

        WayangA2uiTransportResponse response = adapter.exchange(request);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(request.kind()).isEqualTo(WayangA2uiTransportPayloadKind.SURFACE_CATALOG);
        assertThat(response.mimeType()).isEqualTo("application/json");
        assertThat(response.bodyEncoding()).isEqualTo("json");
        assertThat(response.handledCount()).isZero();
        assertThat(response.rejectedCount()).isZero();
        assertThat(response.dataParts()).isEmpty();
        assertThat(response.empty()).isFalse();
        assertThat(response.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.SURFACE_CATALOG.name())
                .containsEntry("responseKind", "surface-catalog")
                .containsEntry("descriptorCount", body.at("/descriptorCount").asInt())
                .containsEntry("surfaceKindCount", body.at("/surfaceKinds").size());
        assertThat(body.at("/surfaceKinds").toString()).contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(body.at("/descriptorCount").asInt()).isGreaterThan(0);
        assertThat(response.body())
                .contains(WayangA2uiActionResult.class.getName())
                .contains(WayangA2uiActionFeedback.class.getName());
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void exchangesSurfaceCatalogViaConvenienceMethod() {
        WayangA2uiTransportAdapter adapter = new WayangA2uiTransportAdapter(new RecordingWayangGollekSdk());

        WayangA2uiTransportResponse response = adapter.exchangeSurfaceCatalog();

        assertThat(response.bodyEncoding()).isEqualTo("json");
        assertThat(response.metadata())
                .containsEntry("requestKind", WayangA2uiTransportPayloadKind.SURFACE_CATALOG.name())
                .containsEntry("responseKind", "surface-catalog");
        assertThat(response.body()).contains(WayangA2uiSurfaceRegistry.RUN_STATUS);
    }

    @Test
    void transportResponseMetadataIsDefensivelyCopied() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name());
        metadata.put("nested", nested);

        WayangA2uiTransportResponse response = new WayangA2uiTransportResponse(
                A2uiProtocol.MIME_TYPE,
                "jsonl",
                "",
                List.of(),
                0,
                0,
                metadata);
        nested.put("value", "changed");
        metadata.put("requestKind", "changed");

        @SuppressWarnings("unchecked")
        Map<String, Object> copiedNested = (Map<String, Object>) response.metadata().get("nested");
        assertThat(response.metadata()).containsEntry("requestKind", WayangA2uiTransportPayloadKind.JSON_LINE.name());
        assertThat(copiedNested).containsEntry("value", "original");
    }

    @Test
    @SuppressWarnings("unchecked")
    void transportResponseDataPartsAreDefensivelyCopied() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "data");
        dataPart.put("nested", nested);
        dataPart.put("ignored", null);

        WayangA2uiTransportResponse response = new WayangA2uiTransportResponse(
                A2uiProtocol.MIME_TYPE,
                "jsonl",
                "",
                List.of(dataPart),
                0,
                0);
        nested.put("value", "changed");
        dataPart.put("kind", "changed");

        Map<String, Object> copiedDataPart = response.dataParts().get(0);
        Map<String, Object> copiedNested = (Map<String, Object>) copiedDataPart.get("nested");
        List<Map<String, Object>> envelopeDataParts =
                (List<Map<String, Object>>) response.toMap().get(WayangA2uiTransportFields.DATA_PARTS);
        Map<String, Object> envelopeNested = (Map<String, Object>) envelopeDataParts.get(0).get("nested");

        assertThat(copiedDataPart)
                .containsEntry("kind", "data")
                .doesNotContainKey("ignored");
        assertThat(copiedNested).containsEntry("value", "original");
        assertThat(envelopeNested).containsEntry("value", "original");
    }

    private static A2uiUserAction action(String name, Map<String, Object> context) {
        return new A2uiUserAction(name, "main", "button", Instant.parse("2026-05-31T00:00:00Z"), context);
    }
}
