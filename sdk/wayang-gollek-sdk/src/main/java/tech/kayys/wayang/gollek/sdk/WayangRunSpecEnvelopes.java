package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire envelope factory for portable Wayang run-spec operations.
 *
 * <p>Run specs are shared across CLI, TUI, HTTP, and automation wrappers, so
 * their JSON shape lives in the SDK alongside the run lifecycle envelopes.</p>
 */
public final class WayangRunSpecEnvelopes {

    private WayangRunSpecEnvelopes() {
    }

    public static Map<String, Object> validation(String path, AgentRunPreview preview) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("path", SdkText.trimToEmpty(path));
        values.put("ready", preview != null && preview.ready());
        values.put("preview", AgentRunEnvelopes.preview(preview));
        return SdkMaps.orderedCopy(values);
    }
}
