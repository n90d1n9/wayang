package tech.kayys.wayang.a2ui.wayang;

import java.util.Objects;

/**
 * Default bridge implementation backed by the transport adapter.
 */
public final class WayangA2uiTransportBridge implements WayangA2uiBridge {

    private final WayangA2uiTransportAdapter adapter;

    public WayangA2uiTransportBridge(WayangA2uiTransportAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    @Override
    public WayangA2uiBridgeResponse exchange(WayangA2uiBridgeRequest request) {
        WayangA2uiBridgeRequest resolved = Objects.requireNonNull(request, "request");
        return WayangA2uiBridgeResponse.of(adapter.exchange(resolved.transportRequest()));
    }

    public WayangA2uiSurfaceCatalog surfaceCatalog() {
        return adapter.surfaceCatalog();
    }
}
