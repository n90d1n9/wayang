package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpActionBindingProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.support.DecodeValues;
import tech.kayys.wayang.a2ui.wayang.support.DecodeCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decodes HTTP action-binding probe responses into canonical probe results.
 */
public final class HttpActionBindingProbeResponseDecoder {

    public static WayangA2uiHttpActionBindingProbeResult from(WayangA2uiHttpResponse response) {
        WayangA2uiHttpResponse resolved = Objects.requireNonNull(response, "response");
        HttpResponseBodyDecoder.Envelope envelope =
                HttpResponseBodyDecoder.lenientJsonEnvelope(
                        resolved.body(),
                        "A2UI action binding report JSON must not be blank",
                        "Unable to decode A2UI action binding report JSON");
        Map<String, Object> metadata = envelope.metadata();
        Map<String, Object> body = envelope.body();
        List<String> policyActions = DecodeCollections.distinctTokens(body.get("policyActions"));
        List<String> handlerActions = DecodeCollections.distinctTokens(body.get("handlerActions"));
        List<String> missingHandlerActions =
                DecodeCollections.distinctTokens(body.get("missingHandlerActions"));
        List<String> orphanHandlerActions =
                DecodeCollections.distinctTokens(body.get("orphanHandlerActions"));
        int missingHandlerCount = DecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.MISSING_HANDLER_COUNT),
                missingHandlerActions.size());
        int orphanHandlerCount = DecodeValues.nonNegativeInt(metadata.get(
                        WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT),
                orphanHandlerActions.size());
        List<Map<String, Object>> issues = HttpActionBindingProbeProjection.issues(missingHandlerActions);
        return new WayangA2uiHttpActionBindingProbeResult(
                resolved.statusCode(),
                resolved.successful(),
                resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION),
                resolved.header(WayangA2uiHttpResponse.HEADER_ALLOW),
                DecodeValues.text(
                        resolved.header(WayangA2uiHttpResponse.HEADER_A2UI_OUTCOME),
                        envelope.outcome()),
                resolved.contentType(),
                envelope.mimeType(),
                envelope.bodyEncoding(),
                DecodeValues.bool(metadata.get(WayangA2uiTransportFields.COMPLETE),
                        DecodeValues.bool(body.get(WayangA2uiTransportFields.COMPLETE), false)),
                DecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.POLICY_ACTION_COUNT),
                        DecodeValues.nonNegativeInt(
                                body.get(WayangA2uiTransportFields.POLICY_ACTION_COUNT),
                                policyActions.size())),
                DecodeValues.nonNegativeInt(metadata.get(
                                WayangA2uiTransportFields.HANDLER_ACTION_COUNT),
                        DecodeValues.nonNegativeInt(
                                body.get(WayangA2uiTransportFields.HANDLER_ACTION_COUNT),
                                handlerActions.size())),
                missingHandlerCount,
                orphanHandlerCount,
                Math.max(missingHandlerCount, issues.size()),
                issues,
                policyActions,
                handlerActions,
                missingHandlerActions,
                orphanHandlerActions,
                metadata,
                body,
                resolved.headers());
    }

    private HttpActionBindingProbeResponseDecoder() {
    }
}
