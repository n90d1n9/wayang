package tech.kayys.wayang.a2ui.core;

/**
 * Stable A2UI v0.8 protocol constants used by Wayang integrations.
 */
public final class A2uiProtocol {

    public static final String VERSION = "v0.8";
    public static final String EXTENSION_URI = "https://a2ui.org/a2a-extension/a2ui/v0.8";
    public static final String MIME_TYPE = "application/json+a2ui";
    public static final String STANDARD_CATALOG_ID =
            "https://a2ui.org/specification/v0_8/standard_catalog_definition.json";
    public static final String CLIENT_CAPABILITIES_KEY = "a2uiClientCapabilities";

    private A2uiProtocol() {
    }
}
