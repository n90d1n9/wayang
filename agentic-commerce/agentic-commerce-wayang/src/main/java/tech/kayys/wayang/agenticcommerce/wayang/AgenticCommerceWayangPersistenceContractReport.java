package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-ready result for an Agentic Commerce persistence store contract run.
 */
public record AgenticCommerceWayangPersistenceContractReport(
        String storeKind,
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceWayangBootstrapConfig bootstrapConfig,
        boolean runtimeConfigPersisted,
        boolean bootstrapConfigPersisted,
        boolean bootstrapReportPersisted,
        boolean manifestPersisted,
        List<String> issues,
        Map<String, Object> storeStatusBefore,
        Map<String, Object> storeStatusAfter,
        Map<String, Object> bootstrapReport,
        Map<String, Object> manifest,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceContractReport {
        storeKind = AgenticCommerceWayangMaps.text(storeKind);
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        bootstrapConfig = bootstrapConfig == null ? AgenticCommerceWayangBootstrapConfig.defaults() : bootstrapConfig;
        issues = AgenticCommerceWayangMaps.stringList(issues);
        storeStatusBefore = AgenticCommerceWayangMaps.copy(storeStatusBefore);
        storeStatusAfter = AgenticCommerceWayangMaps.copy(storeStatusAfter);
        bootstrapReport = AgenticCommerceWayangMaps.copy(bootstrapReport);
        manifest = AgenticCommerceWayangMaps.copy(manifest);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public boolean passed() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int persistedDocumentCount() {
        int count = 0;
        count += runtimeConfigPersisted ? 1 : 0;
        count += bootstrapConfigPersisted ? 1 : 0;
        count += bootstrapReportPersisted ? 1 : 0;
        count += manifestPersisted ? 1 : 0;
        return count;
    }

    public Map<String, Object> persistenceTargetBefore() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(storeStatusBefore, storeKind);
    }

    public Map<String, Object> persistenceTargetAfter() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(storeStatusAfter, storeKind);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed());
        values.put("storeKind", storeKind);
        values.put("issueCount", issueCount());
        values.put("issues", issues);
        values.put("persistedDocumentCount", persistedDocumentCount());
        values.put("runtimeConfigPersisted", runtimeConfigPersisted);
        values.put("bootstrapConfigPersisted", bootstrapConfigPersisted);
        values.put("bootstrapReportPersisted", bootstrapReportPersisted);
        values.put("manifestPersisted", manifestPersisted);
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("bootstrapReport", bootstrapReportSummary());
        values.put("manifest", manifestSummary());
        values.put("persistenceTargetBefore", persistenceTargetBefore());
        values.put("persistenceTargetAfter", persistenceTargetAfter());
        values.put("storeStatusBefore", storeStatusBefore);
        values.put("storeStatusAfter", storeStatusAfter);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private Map<String, Object> bootstrapReportSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        copyValue(values, bootstrapReport, "ready");
        copyValue(values, bootstrapReport, "issueCount");
        copyValue(values, bootstrapReport, "bootstrapIssueCount");
        Object registration = bootstrapReport.get("skillRegistration");
        if (registration instanceof Map<?, ?> map) {
            Map<String, Object> copied = AgenticCommerceWayangMaps.copy(map);
            Map<String, Object> summary = new LinkedHashMap<>();
            copyValue(summary, copied, "successful");
            copyValue(summary, copied, "requestedCount");
            copyValue(summary, copied, "definitionCount");
            copyValue(summary, copied, "runtimeSkillCount");
            copyValue(summary, copied, "missingCount");
            values.put("skillRegistration", Map.copyOf(summary));
        }
        return Map.copyOf(values);
    }

    private Map<String, Object> manifestSummary() {
        Map<String, Object> values = new LinkedHashMap<>();
        copyValue(values, manifest, "skillCount");
        copyValue(values, manifest, "routeCount");
        copyValue(values, manifest, "operationCount");
        copyValue(values, manifest, "skillIds");
        copyValue(values, manifest, "operations");
        return Map.copyOf(values);
    }

    private static void copyValue(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }
}
