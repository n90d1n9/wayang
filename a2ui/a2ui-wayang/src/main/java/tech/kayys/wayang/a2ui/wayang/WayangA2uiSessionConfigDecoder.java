package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.session.SessionConfigDecoder;

import java.util.Map;

/**
 * Public compatibility facade for stored or remote A2UI session configuration decoding.
 */
public final class WayangA2uiSessionConfigDecoder {

    public static WayangA2uiSessionConfig fromMap(Map<?, ?> values) {
        return SessionConfigDecoder.fromMap(values);
    }

    public static WayangA2uiSessionConfig fromJson(String json) {
        return SessionConfigDecoder.fromJson(json);
    }

    private WayangA2uiSessionConfigDecoder() {
    }
}
