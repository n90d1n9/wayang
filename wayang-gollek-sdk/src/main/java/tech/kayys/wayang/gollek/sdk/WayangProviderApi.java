package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

/**
 * Provider capability API for cross-module standards, MCP, RAG, and storage integrations.
 *
 * <p>The API centralizes provider discovery, detail lookup, and JSON envelope
 * rendering so UI wrappers consume the same SDK-owned capability catalog.</p>
 */
public final class WayangProviderApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    WayangProviderApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WayangProviderCapabilityDiscovery discover(WayangProviderCapabilityQuery query) {
        return discover(query, "");
    }

    public WayangProviderCapabilityDiscovery discover(WayangProviderCapabilityQuery query, String search) {
        return sdk.providerCapabilityDiscovery(query, search);
    }

    public List<WayangProviderCapabilityDescriptor> list(WayangProviderCapabilityQuery query) {
        return discover(query).capabilities();
    }

    public WayangProviderCapabilityDescriptor get(String capabilityId) {
        return sdk.providerCapability(capabilityId);
    }

    public Map<String, Object> discoveryEnvelope(WayangProviderCapabilityQuery query) {
        return discoveryEnvelope(discover(query));
    }

    public Map<String, Object> discoveryEnvelope(WayangProviderCapabilityQuery query, String search) {
        return discoveryEnvelope(discover(query, search));
    }

    public Map<String, Object> discoveryEnvelope(WayangProviderCapabilityDiscovery discovery) {
        return WayangProviderCapabilityEnvelopes.discovery(productName(), discovery);
    }

    public Map<String, Object> detailEnvelope(WayangProviderCapabilityDescriptor capability) {
        return WayangProviderCapabilityEnvelopes.detail(productName(), capability);
    }

    public String discoveryJson(WayangProviderCapabilityQuery query) {
        return discoveryJson(discover(query));
    }

    public String discoveryJson(WayangProviderCapabilityQuery query, String search) {
        return discoveryJson(discover(query, search));
    }

    public String discoveryJson(WayangProviderCapabilityDiscovery discovery) {
        return wire.object(discoveryEnvelope(discovery));
    }

    public String detailJson(WayangProviderCapabilityDescriptor capability) {
        return wire.object(detailEnvelope(capability));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
