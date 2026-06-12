package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.child;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

record WayangA2aJsonRpcBindingReportSection(
        String key,
        String path,
        boolean enabled) {

    WayangA2aJsonRpcBindingReportSection {
        key = key == null ? "" : key.trim();
        path = path == null ? "" : path.trim();
    }

    static WayangA2aJsonRpcBindingReportSection from(Map<String, Object> body, String key) {
        return fromMap(key, child(body, key));
    }

    static WayangA2aJsonRpcBindingReportSection fromMap(String key, Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcBindingReportSection(
                key,
                text(copy.get("path"), ""),
                bool(copy.get("enabled"), false));
    }

    boolean pathMissing() {
        return path.isBlank();
    }

    Map<String, Object> missingPathIssue() {
        return issue(
                "bindingReport",
                snakeKey() + "_path_missing",
                key + "Path",
                "non-blank",
                path,
                "A2A JSON-RPC binding report did not expose "
                        + article()
                        + " "
                        + routeName()
                        + " path.");
    }

    private String article() {
        return routeName().matches("^[aeiou].*") ? "an" : "a";
    }

    private String routeName() {
        String normalized = snakeKey().replace('_', ' ').trim();
        return normalized.isBlank() ? "binding report section" : normalized;
    }

    private String snakeKey() {
        if (key.isBlank()) {
            return "";
        }
        return key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
