package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

/**
 * Wire-format API for rendering SDK-owned envelopes.
 *
 * <p>This facade keeps JSON emission in the SDK so UI wrappers do not need their
 * own serializers for Wayang's ordered contract maps.</p>
 */
public final class WayangWireApi {

    WayangWireApi() {
    }

    public String object(Map<String, ?> values) {
        return WayangJson.object(values);
    }

    public String value(Object value) {
        return WayangJson.value(value);
    }

    public String quote(String value) {
        return WayangJson.quote(value);
    }
}
