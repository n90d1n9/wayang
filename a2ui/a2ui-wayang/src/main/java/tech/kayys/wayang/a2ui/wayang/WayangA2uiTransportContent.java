package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiProtocol;

/**
 * Canonical MIME types and body encodings for Wayang A2UI transport responses.
 */
public final class WayangA2uiTransportContent {

    public static final String MIME_A2UI = A2uiProtocol.MIME_TYPE;
    public static final String MIME_JSON = "application/json";
    public static final String MIME_PROBLEM_JSON = "application/problem+json";

    public static final String ENCODING_JSONL = "jsonl";
    public static final String ENCODING_JSON = "json";

    private WayangA2uiTransportContent() {
    }
}
