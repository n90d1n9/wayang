package tech.kayys.wayang.contract;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.Wayang;
import tech.kayys.wayang.client.WayangGollekSdk;
import tech.kayys.wayang.client.WayangWireApi;

/**
 * Contract catalog API for schema discovery and wire-contract export.
 *
 * <p>This facade centralizes contract lookup, JSON Schema bundle creation, and
 * contract envelope rendering so wrappers share one SDK-owned contract source.</p>
 */
public final class WayangContractApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    public WayangContractApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WayangContractDiscovery discover(WayangContractQuery query) {
        return sdk.contractDiscovery(query);
    }

    public WayangContractDiscovery discover(WayangContractKey key) {
        return sdk.contractDiscovery(key);
    }

    public List<WayangContractDescriptor> list(WayangContractQuery query) {
        return discover(query).contracts();
    }

    public WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return sdk.contractJsonSchema(contract);
    }

    public WayangContractJsonSchemaBundle schemaBundle(WayangContractQuery query) {
        return sdk.contractJsonSchemaBundle(query);
    }

    public WayangContractJsonSchemaBundle schemaBundle(WayangContractDiscovery discovery) {
        return sdk.contractJsonSchemaBundle(discovery);
    }

    public WayangContractJsonSchemaBundle schemaBundle(WayangContractKey key) {
        return sdk.contractJsonSchemaBundle(key);
    }

    public WayangContractIntegrityReport integrity() {
        return sdk.contractIntegrity();
    }

    public WayangContractCommandCoverageReport coverage() {
        return sdk.contractCommandCoverage();
    }

    public Map<String, Object> catalogEnvelope(WayangContractQuery query) {
        return catalogEnvelope(discover(query));
    }

    public Map<String, Object> catalogEnvelope(WayangContractDiscovery discovery) {
        return WayangContractEnvelopes.catalog(productName(), discovery);
    }

    public Map<String, Object> indexEnvelope(WayangContractQuery query) {
        return indexEnvelope(discover(query));
    }

    public Map<String, Object> indexEnvelope(WayangContractDiscovery discovery) {
        return WayangContractEnvelopes.index(productName(), discovery);
    }

    public Map<String, Object> schemaBundleEnvelope(WayangContractQuery query) {
        return schemaBundleEnvelope(schemaBundle(query));
    }

    public Map<String, Object> schemaBundleEnvelope(WayangContractDiscovery discovery) {
        return schemaBundleEnvelope(schemaBundle(discovery));
    }

    public Map<String, Object> schemaBundleEnvelope(WayangContractKey key) {
        return schemaBundleEnvelope(schemaBundle(key));
    }

    public Map<String, Object> schemaBundleEnvelope(WayangContractJsonSchemaBundle bundle) {
        return WayangContractEnvelopes.schemaBundle(productName(), bundle);
    }

    public Map<String, Object> schemaDocument(WayangContractDescriptor contract) {
        return WayangContractEnvelopes.schemaDocument(schema(contract));
    }

    public Map<String, Object> schemaDocument(WayangContractKey key) {
        return WayangContractEnvelopes.schemaDocument(schemaBundle(key).schemaByKey(key).orElse(null));
    }

    public String catalogJson(WayangContractQuery query) {
        return catalogJson(discover(query));
    }

    public String catalogJson(WayangContractDiscovery discovery) {
        return wire.object(catalogEnvelope(discovery));
    }

    public String indexJson(WayangContractQuery query) {
        return indexJson(discover(query));
    }

    public String indexJson(WayangContractDiscovery discovery) {
        return wire.object(indexEnvelope(discovery));
    }

    public String schemaBundleJson(WayangContractQuery query) {
        return schemaBundleJson(schemaBundle(query));
    }

    public String schemaBundleJson(WayangContractDiscovery discovery) {
        return schemaBundleJson(schemaBundle(discovery));
    }

    public String schemaBundleJson(WayangContractKey key) {
        return schemaBundleJson(schemaBundle(key));
    }

    public String schemaBundleJson(WayangContractJsonSchemaBundle bundle) {
        return wire.object(schemaBundleEnvelope(bundle));
    }

    public String schemaJson(WayangContractDescriptor contract) {
        return wire.object(schemaDocument(contract));
    }

    public String schemaJson(WayangContractKey key) {
        return wire.object(schemaDocument(key));
    }

    public String integrityJson(WayangContractIntegrityReport report) {
        return wire.object(WayangContractEnvelopes.integrityReport(productName(), report));
    }

    public String coverageJson(WayangContractCommandCoverageReport report) {
        return wire.object(WayangContractEnvelopes.coverageReport(productName(), report));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
