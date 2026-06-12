package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.action.ActionContextReader;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSource;
import tech.kayys.wayang.a2ui.wayang.support.RecordCollections;

import tech.kayys.wayang.a2ui.core.A2uiClientMessage;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Session boundary that decodes inbound A2UI payloads and routes them to Wayang.
 */
public final class WayangA2uiSession {

    private final WayangA2uiActionRouter router;
    private final A2uiJsonlCodec codec;
    private final WayangA2uiSessionConfig config;
    private final WayangA2uiSessionState state;

    public WayangA2uiSession(WayangGollekSdk sdk) {
        this(sdk, WayangA2uiSessionConfig.defaultConfig());
    }

    public WayangA2uiSession(WayangGollekSdk sdk, WayangA2uiActionPolicy policy) {
        this(sdk, policy, null);
    }

    public WayangA2uiSession(
            WayangGollekSdk sdk,
            WayangA2uiActionPolicy policy,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        this(new WayangA2uiActionRouter(sdk, policy, surfaceRegistry), new A2uiJsonlCodec(),
                new WayangA2uiSessionConfig(true, policy),
                new WayangA2uiSessionState());
    }

    public WayangA2uiSession(WayangGollekSdk sdk, WayangA2uiSessionConfig config) {
        this(sdk, config, null);
    }

    public WayangA2uiSession(WayangGollekSdk sdk, SessionConfigSource configSource) {
        this(sdk, WayangA2uiSessionConfig.fromSource(configSource));
    }

    public WayangA2uiSession(
            WayangGollekSdk sdk,
            WayangA2uiSessionConfig config,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        this(new WayangA2uiActionRouter(
                sdk,
                configOrDefault(config).actionPolicy(),
                surfaceRegistry),
                new A2uiJsonlCodec(),
                configOrDefault(config),
                new WayangA2uiSessionState());
    }

    public WayangA2uiSession(
            WayangGollekSdk sdk,
            SessionConfigSource configSource,
            WayangA2uiSurfaceRegistry surfaceRegistry) {
        this(sdk, WayangA2uiSessionConfig.fromSource(configSource), surfaceRegistry);
    }

    public WayangA2uiSession(WayangA2uiActionRouter router) {
        this(router, new A2uiJsonlCodec());
    }

    public WayangA2uiSession(WayangA2uiActionRouter router, A2uiJsonlCodec codec) {
        this(router, codec, WayangA2uiSessionConfig.defaultConfig(), new WayangA2uiSessionState());
    }

    private WayangA2uiSession(
            WayangA2uiActionRouter router,
            A2uiJsonlCodec codec,
            WayangA2uiSessionConfig config,
            WayangA2uiSessionState state) {
        this.router = Objects.requireNonNull(router, "router");
        this.codec = codec == null ? new A2uiJsonlCodec() : codec;
        this.config = configOrDefault(config);
        this.state = state == null ? new WayangA2uiSessionState() : state;
    }

    public WayangA2uiSessionResult handleJsonLine(String line) {
        return handle(codec.clientMessage(line));
    }

    public WayangA2uiSessionResult handleJsonl(String jsonl) {
        return handle(codec.clientStream(jsonl));
    }

    public WayangA2uiSessionResult handleDataPart(String dataPart) {
        return handle(codec.clientDataPart(dataPart));
    }

    public WayangA2uiSessionResult handleDataPart(Map<?, ?> dataPart) {
        return handle(codec.clientDataPart(dataPart));
    }

    public WayangA2uiSessionResult handle(A2uiClientMessage message) {
        return handle(List.of(Objects.requireNonNull(message, "message")));
    }

    public WayangA2uiSessionResult handle(List<? extends A2uiClientMessage> messages) {
        List<WayangA2uiActionResult> results = RecordCollections.nonNullList(messages).stream()
                .map(state::apply)
                .map(this::route)
                .toList();
        results.forEach(state::observe);
        return WayangA2uiSessionResult.of(results, codec, surfaceRegistry());
    }

    public WayangA2uiSessionState state() {
        return state;
    }

    public WayangA2uiSurfaceRegistry surfaceRegistry() {
        return router.surfaceRegistry();
    }

    public WayangA2uiSurfaceCatalog surfaceCatalog() {
        return surfaceRegistry().surfaceCatalog();
    }

    public WayangA2uiActionBindingReport actionBindingReport() {
        return router.actionBindingReport();
    }

    private WayangA2uiActionResult route(A2uiClientMessage message) {
        if (config.enabled()) {
            return router.route(message);
        }
        if (message instanceof A2uiUserAction action) {
            return WayangA2uiActionResult.rejected(
                    action.name(),
                    ActionContextReader.text(action, "runId"),
                    "A2UI session is disabled.");
        }
        return WayangA2uiActionResult.rejected("", "", "A2UI session is disabled.");
    }

    private static WayangA2uiSessionConfig configOrDefault(WayangA2uiSessionConfig config) {
        return config == null ? WayangA2uiSessionConfig.defaultConfig() : config;
    }
}
