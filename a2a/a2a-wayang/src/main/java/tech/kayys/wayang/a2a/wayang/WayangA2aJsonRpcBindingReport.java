package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-ready diagnostics for the A2A JSON-RPC HTTP binding surface.
 */
public record WayangA2aJsonRpcBindingReport(
        WayangA2aJsonRpcHttpConfig config,
        List<String> methods,
        WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage,
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot) {

    public static final String OPERATION_JSON_RPC_BINDING_REPORT = "JsonRpcBindingReport";

    public WayangA2aJsonRpcBindingReport {
        config = Objects.requireNonNull(config, "config");
        methods = normalizeMethods(methods);
    }

    public WayangA2aJsonRpcBindingReport(
            WayangA2aJsonRpcHttpConfig config,
            List<String> methods,
            WayangA2aJsonRpcMethodDispatchCoverage methodDispatchCoverage) {
        this(config, methods, methodDispatchCoverage, null);
    }

    public WayangA2aJsonRpcBindingReport(
            WayangA2aJsonRpcHttpConfig config,
            List<String> methods) {
        this(config, methods, null);
    }

    public static WayangA2aJsonRpcBindingReport defaults() {
        return fromConfig(WayangA2aJsonRpcHttpConfig.defaults());
    }

    public static WayangA2aJsonRpcBindingReport from(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return new WayangA2aJsonRpcBindingReport(
                resolved.config(),
                WayangA2aJsonRpcMethods.methods(),
                resolved.methodDispatchCoverage(),
                resolved.methodHandlerRegistrySnapshot());
    }

    public static WayangA2aJsonRpcBindingReport fromConfig(WayangA2aJsonRpcHttpConfig config) {
        return new WayangA2aJsonRpcBindingReport(config, WayangA2aJsonRpcMethods.methods());
    }

    public int methodCount() {
        return methods.size();
    }

    public List<String> streamingMethods() {
        return methods.stream()
                .filter(WayangA2aJsonRpcMethods::streaming)
                .toList();
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcBindingReportProjection.response(this);
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcBindingReportProjection.report(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    private static List<String> normalizeMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return WayangA2aJsonRpcMethods.methods();
        }
        return methods.stream()
                .map(method -> WayangA2aMaps.required(method, "method"))
                .distinct()
                .peek(WayangA2aJsonRpcBindingReport::requireSupported)
                .toList();
    }

    private static void requireSupported(String method) {
        WayangA2aJsonRpcMethods.requireDescriptor(method);
    }
}
