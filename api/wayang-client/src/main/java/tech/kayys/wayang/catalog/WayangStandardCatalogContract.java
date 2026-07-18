package tech.kayys.wayang.catalog;

import tech.kayys.wayang.client.SdkText;

public record WayangStandardCatalogContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.standard.catalog";
    public static final int VERSION = 1;
    public static final String STANDARDS_CATALOG = "standards-catalog";

    public WayangStandardCatalogContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangStandardCatalogContract standardsCatalog() {
        return new WayangStandardCatalogContract(SCHEMA, VERSION, STANDARDS_CATALOG);
    }
}
