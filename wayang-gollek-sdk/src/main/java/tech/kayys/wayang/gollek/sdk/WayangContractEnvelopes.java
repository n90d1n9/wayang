package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire envelope factory for contract catalogs, schemas, and contract health reports.
 *
 * <p>Contract envelopes are kept in the SDK so CLIs, remote APIs, TUI surfaces,
 * and embedded products share the same ordered payload shape and fallback model.</p>
 */
public final class WayangContractEnvelopes {

    private WayangContractEnvelopes() {
    }

    public static Map<String, Object> catalog(String productName, WayangContractDiscovery discovery) {
        WayangContractDiscovery model = normalize(discovery);
        Map<String, Object> values = new LinkedHashMap<>(summary(productName, model));
        values.put("contracts", model.contracts().stream()
                .map(WayangContractEnvelopes::contract)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> index(String productName, WayangContractDiscovery discovery) {
        return summary(productName, normalize(discovery));
    }

    public static Map<String, Object> schemaBundle(String productName, WayangContractJsonSchemaBundle bundle) {
        WayangContractJsonSchemaBundle model = bundle == null
                ? new WayangContractJsonSchemaBundle(normalize(null), List.of())
                : bundle;
        WayangContractDiscovery discovery = normalize(model.discovery());
        Map<String, Object> values = new LinkedHashMap<>(summary(productName, discovery));
        values.put("schemaCount", model.schemas().size());
        values.put("schemaIds", model.ids());
        values.put("schemaDocumentsById", model.documentsById());
        values.put("schemaDocuments", model.schemas().stream()
                .map(WayangContractEnvelopes::schemaEntry)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> schemaDocument(WayangContractJsonSchema schema) {
        return schema == null ? Map.of() : schema.document();
    }

    public static Map<String, Object> integrityReport(String productName, WayangContractIntegrityReport report) {
        WayangContractIntegrityReport model = normalizeIntegrityReport(report);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("valid", model.valid());
        values.put("issueCount", model.issueCount());
        values.put("totalContracts", model.totalContracts());
        values.put("totalCommands", model.totalCommands());
        values.put("contractCommandLinks", model.contractCommandLinks());
        values.put("commandContractLinks", model.commandContractLinks());
        values.put("issues", model.issues().stream()
                .map(WayangContractEnvelopes::integrityIssue)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> integrityIssue(WayangContractIntegrityIssue issue) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", issue.kind());
        values.put("message", issue.message());
        values.put("schema", issue.schema());
        values.put("version", issue.version());
        values.put("envelope", issue.envelope());
        values.put("commandId", issue.commandId());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> coverageReport(
            String productName,
            WayangContractCommandCoverageReport report) {
        WayangContractCommandCoverageReport model = normalizeCoverageReport(report);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("totalContracts", model.totalContracts());
        values.put("totalCommands", model.totalCommands());
        values.put("commandLinkedContracts", model.commandLinkedContracts());
        values.put("commandlessContracts", model.commandlessContracts());
        values.put("incompleteContracts", model.incompleteContracts());
        values.put("commandContractLinks", model.commandContractLinks());
        values.put("commandlessEntries", coverageEntries(model.commandlessEntries()));
        values.put("incompleteEntries", coverageEntries(model.incompleteEntries()));
        return SdkMaps.orderedCopy(values);
    }

    public static List<Map<String, Object>> coverageEntries(List<WayangContractCommandCoverageEntry> entries) {
        return SdkLists.copy(entries).stream()
                .map(WayangContractEnvelopes::coverageEntry)
                .toList();
    }

    public static Map<String, Object> coverageEntry(WayangContractCommandCoverageEntry entry) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", entry.schema());
        values.put("version", entry.version());
        values.put("envelope", entry.envelope());
        values.put("domain", entry.domain());
        values.put("jsonSchemaId", entry.jsonSchemaId());
        values.put("declaredCommandIds", entry.declaredCommandIds());
        values.put("linkedCommandIds", entry.linkedCommandIds());
        values.put("unlinkedCommandIds", entry.unlinkedCommandIds());
        values.put("undeclaredLinkedCommandIds", entry.undeclaredLinkedCommandIds());
        values.put("commandLinked", entry.commandLinked());
        values.put("commandless", entry.commandless());
        values.put("complete", entry.complete());
        return SdkMaps.orderedCopy(values);
    }

    public static WayangContractIntegrityReport normalizeIntegrityReport(WayangContractIntegrityReport report) {
        return report == null ? new WayangContractIntegrityReport(0, 0, 0, 0, List.of()) : report;
    }

    public static WayangContractCommandCoverageReport normalizeCoverageReport(
            WayangContractCommandCoverageReport report) {
        return report == null ? new WayangContractCommandCoverageReport(0, 0, List.of()) : report;
    }

    public static Map<String, Object> summary(String productName, WayangContractDiscovery model) {
        WayangContractDiscovery normalized = normalize(model);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("schema", normalized.query().schema());
        values.put("envelope", normalized.query().envelope());
        values.put("commandId", normalized.query().commandId());
        values.put("domain", normalized.query().domain());
        values.put("jsonSchemaId", normalized.query().jsonSchemaId());
        values.put("query", query(normalized.query()));
        values.put("totalContracts", normalized.totalContracts());
        values.put("matchingContracts", normalized.matchingContracts());
        values.put("schemas", normalized.schemas());
        values.put("schemaCounts", normalized.schemaCounts());
        values.put("schemaSummaries", normalized.schemaSummaries().stream()
                .map(WayangContractEnvelopes::facetSummary)
                .toList());
        values.put("domains", normalized.domains());
        values.put("domainCounts", normalized.domainCounts());
        values.put("domainSummaries", normalized.domainSummaries().stream()
                .map(WayangContractEnvelopes::facetSummary)
                .toList());
        values.put("envelopes", normalized.envelopes());
        values.put("envelopeSummaries", normalized.envelopeSummaries().stream()
                .map(WayangContractEnvelopes::facetSummary)
                .toList());
        values.put("jsonSchemaIds", normalized.jsonSchemaIds());
        values.put("commandIds", normalized.commandIds());
        values.put("commandIdCounts", normalized.commandIdCounts());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> query(WayangContractQuery query) {
        WayangContractQuery normalized = query == null ? WayangContractQuery.all() : query;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", normalized.schema());
        values.put("envelope", normalized.envelope());
        values.put("commandId", normalized.commandId());
        values.put("domain", normalized.domain());
        values.put("jsonSchemaId", normalized.jsonSchemaId());
        values.put("filtered", normalized.filtered());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> facetSummary(WayangContractFacetSummary summary) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", summary.name());
        values.put("count", summary.count());
        values.put("schemas", summary.schemas());
        values.put("domains", summary.domains());
        values.put("envelopes", summary.envelopes());
        values.put("jsonSchemaIds", summary.jsonSchemaIds());
        values.put("commandIds", summary.commandIds());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> contract(WayangContractDescriptor contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", contract.schema());
        values.put("version", contract.version());
        values.put("envelope", contract.envelope());
        values.put("domain", contract.domain());
        values.put("jsonSchemaId", contract.jsonSchemaId());
        values.put("description", contract.description());
        values.put("commandIds", contract.commandIds());
        values.put("commands", contract.commands());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> schemaEntry(WayangContractJsonSchema schema) {
        WayangContractDescriptor contract = schema.contract();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", schema.id());
        values.put("schema", contract.schema());
        values.put("version", contract.version());
        values.put("envelope", contract.envelope());
        values.put("domain", contract.domain());
        values.put("commandIds", contract.commandIds());
        values.put("document", schema.document());
        return SdkMaps.orderedCopy(values);
    }

    public static WayangContractDiscovery normalize(WayangContractDiscovery discovery) {
        return discovery == null
                ? WayangContractDiscovery.of(WayangContractQuery.all(), List.of(), 0)
                : discovery;
    }
}
