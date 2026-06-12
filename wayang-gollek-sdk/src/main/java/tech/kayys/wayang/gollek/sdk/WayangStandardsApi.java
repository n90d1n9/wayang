package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

/**
 * Standards API for catalog and provider-alignment health surfaces.
 *
 * <p>This facade keeps standards catalog lookup, alignment assessment, and JSON
 * envelope rendering in the SDK for CLI, HTTP, and product shells.</p>
 */
public final class WayangStandardsApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    WayangStandardsApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WayangStandardCatalog catalog() {
        return sdk.standardsCatalog();
    }

    public WayangStandardAlignmentHealthReport health(WayangStandardAlignmentPolicyConfig config) {
        return sdk.standardAlignmentHealth(config);
    }

    public Map<String, Object> catalogEnvelope(WayangStandardCatalog catalog) {
        return WayangStandardCatalogEnvelopes.catalog(productName(), catalog);
    }

    public Map<String, Object> healthEnvelope(WayangStandardAlignmentHealthReport health) {
        return WayangStandardAlignmentHealthEnvelopes.health(productName(), health);
    }

    public String catalogJson(WayangStandardCatalog catalog) {
        return wire.object(catalogEnvelope(catalog));
    }

    public String healthJson(WayangStandardAlignmentHealthReport health) {
        return wire.object(healthEnvelope(health));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
