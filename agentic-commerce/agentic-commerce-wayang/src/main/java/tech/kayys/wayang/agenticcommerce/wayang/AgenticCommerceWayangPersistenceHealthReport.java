package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Non-mutating health report for an Agentic Commerce Wayang persistence store.
 */
public record AgenticCommerceWayangPersistenceHealthReport(
        String storageKind,
        AgenticCommerceWayangPersistenceCapabilities capabilities,
        boolean statusReadable,
        boolean runtimeConfigAvailable,
        boolean bootstrapConfigAvailable,
        boolean bootstrapReportAvailable,
        boolean manifestAvailable,
        List<AgenticCommerceWayangPersistenceDocumentStatus> documents,
        List<String> issues,
        List<String> warnings,
        Map<String, Object> storeStatus,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceHealthReport {
        storageKind = AgenticCommerceWayangMaps.text(storageKind);
        capabilities = capabilities == null
                ? AgenticCommerceWayangPersistenceCapabilities.fromStatus(storeStatus)
                : capabilities;
        documents = normalizeDocuments(documents);
        issues = AgenticCommerceWayangMaps.stringList(issues);
        warnings = AgenticCommerceWayangMaps.stringList(warnings);
        storeStatus = AgenticCommerceWayangMaps.copy(storeStatus);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceHealthReport from(
            AgenticCommerceWayangPersistenceStore store) {
        AgenticCommerceWayangPersistenceStore resolved = Objects.requireNonNull(store, "store");
        List<String> issues = new ArrayList<>();
        Map<String, Object> storeStatus = storeStatus(resolved, issues);
        AgenticCommerceWayangPersistenceCapabilities capabilities =
                AgenticCommerceWayangPersistenceCapabilities.fromStatus(storeStatus);
        List<AgenticCommerceWayangPersistenceDocumentStatus> documents = documentStatuses(
                resolved,
                storeStatus);
        AgenticCommerceWayangPersistenceDocumentHealthIndex documentIndex =
                AgenticCommerceWayangPersistenceDocumentHealthIndex.from(documents);
        documents.stream()
                .flatMap(document -> document.issues().stream())
                .forEach(issues::add);
        List<String> warnings = warnings(capabilities, documents);
        return new AgenticCommerceWayangPersistenceHealthReport(
                resolved.storageKind(),
                capabilities,
                !Boolean.FALSE.equals(storeStatus.get("statusReadable")),
                documentIndex.available(AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG),
                documentIndex.available(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG),
                documentIndex.available(AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT),
                documentIndex.available(AgenticCommerceWayangPersistenceDocuments.MANIFEST),
                documents,
                issues,
                warnings,
                storeStatus,
                attributes(capabilities));
    }

    public boolean ready() {
        return issues.isEmpty();
    }

    public boolean complete() {
        return availableDocumentCount() == requiredDocumentCount();
    }

    public String healthStatus() {
        return summary().healthStatus();
    }

    public AgenticCommerceWayangPersistenceHealthSummary summary() {
        return AgenticCommerceWayangPersistenceHealthSummary.from(this);
    }

    public AgenticCommerceWayangPersistenceDocumentHealthIndex documentIndex() {
        return AgenticCommerceWayangPersistenceDocumentHealthIndex.from(documents);
    }

    public Optional<AgenticCommerceWayangPersistenceDocumentStatus> documentStatus(String id) {
        return documentIndex().status(id);
    }

    public Optional<AgenticCommerceWayangPersistenceDocumentStatus> documentStatus(
            AgenticCommerceWayangPersistenceDocument document) {
        return documentIndex().status(document);
    }

    public int issueCount() {
        return issues.size();
    }

    public int warningCount() {
        return warnings.size();
    }

    public int findingCount() {
        return findings().size();
    }

    public int errorFindingCount() {
        return errorFindings().size();
    }

    public int warningFindingCount() {
        return warningFindings().size();
    }

    public int requiredDocumentCount() {
        return AgenticCommerceWayangPersistenceDocuments.count();
    }

    public int availableDocumentCount() {
        return (int) documents.stream()
                .filter(AgenticCommerceWayangPersistenceDocumentStatus::available)
                .count();
    }

    public List<AgenticCommerceWayangPersistenceDocumentStatus> missingDocuments() {
        return documentIndex().missing();
    }

    public List<AgenticCommerceWayangPersistenceDocumentStatus> failedDocuments() {
        return documentIndex().failed();
    }

    public List<AgenticCommerceWayangPersistenceHealthFinding> findings() {
        List<AgenticCommerceWayangPersistenceHealthFinding> values = new ArrayList<>();
        storeIssues().stream()
                .map(AgenticCommerceWayangPersistenceHealthFinding::storeError)
                .forEach(values::add);
        documents.forEach(document -> document.issues().stream()
                .map(issue -> AgenticCommerceWayangPersistenceHealthFinding.documentError(document, issue))
                .forEach(values::add));
        storeWarnings().stream()
                .map(AgenticCommerceWayangPersistenceHealthFinding::storeWarning)
                .forEach(values::add);
        documents.forEach(document -> document.warnings().stream()
                .map(warning -> AgenticCommerceWayangPersistenceHealthFinding.documentWarning(document, warning))
                .forEach(values::add));
        return List.copyOf(values);
    }

    public List<AgenticCommerceWayangPersistenceHealthFinding> errorFindings() {
        return findings().stream()
                .filter(AgenticCommerceWayangPersistenceHealthFinding::error)
                .toList();
    }

    public List<AgenticCommerceWayangPersistenceHealthFinding> warningFindings() {
        return findings().stream()
                .filter(AgenticCommerceWayangPersistenceHealthFinding::warning)
                .toList();
    }

    public Map<String, Object> persistenceTarget() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(storeStatus, storageKind);
    }

    public Map<String, Object> toMap() {
        AgenticCommerceWayangPersistenceHealthSummary summary = summary();
        AgenticCommerceWayangPersistenceDocumentHealthIndex documentIndex = documentIndex();
        List<AgenticCommerceWayangPersistenceHealthFinding> findings = findings();
        Map<String, Object> persistenceTarget = persistenceTarget();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("complete", complete());
        values.put("healthStatus", summary.healthStatus());
        values.put("storageKind", storageKind);
        values.put("statusReadable", statusReadable);
        values.put("runtimeConfigAvailable", runtimeConfigAvailable);
        values.put("bootstrapConfigAvailable", bootstrapConfigAvailable);
        values.put("bootstrapReportAvailable", bootstrapReportAvailable);
        values.put("manifestAvailable", manifestAvailable);
        values.put("availableDocumentCount", availableDocumentCount());
        values.put("requiredDocumentCount", requiredDocumentCount());
        values.put("issueCount", issueCount());
        values.put("warningCount", warningCount());
        values.put("findingCount", findings.size());
        values.put("errorFindingCount", errorFindings().size());
        values.put("warningFindingCount", warningFindings().size());
        values.put("issues", issues);
        values.put("warnings", warnings);
        values.put("findings", findings.stream()
                .map(AgenticCommerceWayangPersistenceHealthFinding::toMap)
                .toList());
        values.put("summary", summary.toMap());
        values.put("documentIndex", documentIndex.toMap());
        values.put("documents", documents.stream()
                .map(AgenticCommerceWayangPersistenceDocumentStatus::toMap)
                .toList());
        values.put("capabilities", capabilities.toMap());
        values.put("persistenceTarget", persistenceTarget);
        values.put("storeStatus", storeStatus);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> storeStatus(
            AgenticCommerceWayangPersistenceStore store,
            List<String> issues) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            values.putAll(store.toMap());
            values.put("statusReadable", true);
        } catch (RuntimeException exception) {
            issues.add("store_status_failed");
            values.put("statusReadable", false);
        }
        values.putIfAbsent("storageKind", store.storageKind());
        return Map.copyOf(values);
    }

    private static <T> boolean present(
            AgenticCommerceWayangPersistenceStore store,
            Function<AgenticCommerceWayangPersistenceStore, Optional<T>> loader,
            List<String> issues,
            String issue) {
        try {
            return loader.apply(store).isPresent();
        } catch (RuntimeException exception) {
            issues.add(issue);
            return false;
        }
    }

    private static List<String> warnings(
            AgenticCommerceWayangPersistenceCapabilities capabilities,
            List<AgenticCommerceWayangPersistenceDocumentStatus> documents) {
        List<String> values = new ArrayList<>();
        if (capabilities.ephemeral()) {
            values.add("persistence_store_ephemeral");
        }
        if (!capabilities.durable()) {
            values.add("persistence_store_not_durable");
        }
        if (capabilities.hybrid() && !capabilities.mirrored()) {
            values.add("hybrid_writes_not_mirrored");
        }
        documents.stream()
                .flatMap(document -> document.warnings().stream())
                .forEach(values::add);
        return List.copyOf(values);
    }

    private static List<AgenticCommerceWayangPersistenceDocumentStatus> documentStatuses(
            AgenticCommerceWayangPersistenceStore store,
            Map<String, Object> storeStatus) {
        return List.of(
                documentStatus(
                        AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG,
                        store,
                        AgenticCommerceWayangPersistenceStore::loadRuntimeConfig,
                        storeStatus),
                documentStatus(
                        AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG,
                        store,
                        AgenticCommerceWayangPersistenceStore::loadBootstrapConfig,
                        storeStatus),
                documentStatus(
                        AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT,
                        store,
                        AgenticCommerceWayangPersistenceStore::loadBootstrapReport,
                        storeStatus),
                documentStatus(
                        AgenticCommerceWayangPersistenceDocuments.MANIFEST,
                        store,
                        AgenticCommerceWayangPersistenceStore::loadManifest,
                        storeStatus));
    }

    private static <T> AgenticCommerceWayangPersistenceDocumentStatus documentStatus(
            AgenticCommerceWayangPersistenceDocument document,
            AgenticCommerceWayangPersistenceStore store,
            Function<AgenticCommerceWayangPersistenceStore, Optional<T>> loader,
            Map<String, Object> storeStatus) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean available = present(store, loader, issues, document.loadFailureIssue());
        boolean loadable = issues.isEmpty();
        if (!available) {
            warnings.add(document.missingWarning());
        }
        return new AgenticCommerceWayangPersistenceDocumentStatus(
                document,
                available,
                loadable,
                issues,
                warnings,
                documentAttributes(document, storeStatus));
    }

    private static Map<String, Object> documentAttributes(
            AgenticCommerceWayangPersistenceDocument document,
            Map<String, Object> storeStatus) {
        Map<String, Object> values = new LinkedHashMap<>();
        copyKnown(values, "path", storeStatus, document.pathStatusKey());
        copyKnown(values, "objectKey", storeStatus, document.objectKeyStatusKey());
        copyKnown(values, "statusAvailable", storeStatus, document.availabilityStatusKey());
        return Map.copyOf(values);
    }

    private static void copyKnown(
            Map<String, Object> values,
            String targetKey,
            Map<String, Object> source,
            String sourceKey) {
        if (source.containsKey(sourceKey)) {
            values.put(targetKey, source.get(sourceKey));
        }
    }

    private List<String> storeIssues() {
        List<String> documentIssues = documents.stream()
                .flatMap(document -> document.issues().stream())
                .toList();
        return issues.stream()
                .filter(issue -> !documentIssues.contains(issue))
                .toList();
    }

    private List<String> storeWarnings() {
        List<String> documentWarnings = documents.stream()
                .flatMap(document -> document.warnings().stream())
                .toList();
        return warnings.stream()
                .filter(warning -> !documentWarnings.contains(warning))
                .toList();
    }

    private static List<AgenticCommerceWayangPersistenceDocumentStatus> normalizeDocuments(
            List<AgenticCommerceWayangPersistenceDocumentStatus> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static Map<String, Object> attributes(AgenticCommerceWayangPersistenceCapabilities capabilities) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("durable", capabilities.durable());
        values.put("ephemeral", capabilities.ephemeral());
        values.put("cloudStorage", capabilities.cloudStorage());
        values.put("hybrid", capabilities.hybrid());
        return Map.copyOf(values);
    }
}
