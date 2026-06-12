package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mutation-free preview of a persistence transfer.
 */
public record AgenticCommerceWayangPersistenceTransferPlan(
        String sourceStoreKind,
        String targetStoreKind,
        AgenticCommerceWayangPersistenceTransferOptions options,
        List<String> plannedDocuments,
        List<String> skippedDocuments,
        List<String> blockedDocuments,
        List<String> issues,
        Map<String, Object> sourceStatus,
        Map<String, Object> targetStatus,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferPlan {
        sourceStoreKind = AgenticCommerceWayangMaps.text(sourceStoreKind);
        targetStoreKind = AgenticCommerceWayangMaps.text(targetStoreKind);
        options = options == null ? AgenticCommerceWayangPersistenceTransferOptions.defaults() : options;
        plannedDocuments = AgenticCommerceWayangMaps.stringList(plannedDocuments);
        skippedDocuments = AgenticCommerceWayangMaps.stringList(skippedDocuments);
        blockedDocuments = AgenticCommerceWayangMaps.stringList(blockedDocuments);
        issues = AgenticCommerceWayangMaps.stringList(issues);
        sourceStatus = AgenticCommerceWayangMaps.copy(sourceStatus);
        targetStatus = AgenticCommerceWayangMaps.copy(targetStatus);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public static AgenticCommerceWayangPersistenceTransferPlan from(
            AgenticCommerceWayangPersistenceTransferReport report,
            AgenticCommerceWayangPersistenceTransferOptions transferOptions) {
        AgenticCommerceWayangPersistenceTransferReport resolved = Objects.requireNonNull(report, "report");
        AgenticCommerceWayangPersistenceTransferOptions options = transferOptions == null
                ? resolved.options()
                : transferOptions;
        return new AgenticCommerceWayangPersistenceTransferPlan(
                resolved.sourceStoreKind(),
                resolved.targetStoreKind(),
                options,
                resolved.plannedDocuments(),
                resolved.skippedDocuments(),
                resolved.blockedDocuments(),
                resolved.issues(),
                resolved.sourceStatus(),
                resolved.targetStatusBefore(),
                planAttributes(resolved, options));
    }

    public boolean passed() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int plannedDocumentCount() {
        return plannedDocuments.size();
    }

    public int skippedDocumentCount() {
        return skippedDocuments.size();
    }

    public int blockedDocumentCount() {
        return blockedDocuments.size();
    }

    public int copyableDocumentCount() {
        Set<String> blocked = new HashSet<>(blockedDocuments);
        int count = 0;
        for (String document : plannedDocuments) {
            if (!blocked.contains(document)) {
                count++;
            }
        }
        return count;
    }

    public boolean wouldMutateTarget() {
        return passed() && !options.dryRun() && copyableDocumentCount() > 0;
    }

    public List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents() {
        return AgenticCommerceWayangPersistenceDocuments.ALL.stream()
                .map(document -> AgenticCommerceWayangPersistenceTransferDocumentStatus.from(
                        document,
                        plannedDocuments,
                        List.of(),
                        skippedDocuments,
                        blockedDocuments,
                        issues,
                        options.dryRun(),
                        true))
                .toList();
    }

    public Map<String, Object> sourcePersistenceTarget() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(sourceStatus, sourceStoreKind);
    }

    public Map<String, Object> targetPersistenceTarget() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(targetStatus, targetStoreKind);
    }

    public AgenticCommerceWayangPersistenceTargetComparison persistenceTargetComparison() {
        return AgenticCommerceWayangPersistenceTargetComparison.between(
                "source",
                sourcePersistenceTarget(),
                "target",
                targetPersistenceTarget());
    }

    public AgenticCommerceWayangPersistenceTransferSummary summary() {
        return AgenticCommerceWayangPersistenceTransferSummary.from(this);
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> findings() {
        return AgenticCommerceWayangPersistenceTransferFindings.from(this);
    }

    public AgenticCommerceWayangPersistenceTransferFindingIndex findingIndex() {
        return AgenticCommerceWayangPersistenceTransferFindingIndex.from(findings());
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> errorFindings() {
        return findingIndex().errors();
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> warningFindings() {
        return findingIndex().warnings();
    }

    public List<AgenticCommerceWayangPersistenceTransferFinding> infoFindings() {
        return findingIndex().infos();
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

    public int infoFindingCount() {
        return infoFindings().size();
    }

    public Map<String, Object> toMap() {
        AgenticCommerceWayangPersistenceTransferSummary summary = summary();
        List<AgenticCommerceWayangPersistenceTransferFinding> findings = findings();
        AgenticCommerceWayangPersistenceTransferFindingIndex findingIndex =
                AgenticCommerceWayangPersistenceTransferFindingIndex.from(findings);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("planningOnly", true);
        values.put("passed", passed());
        values.put("transferStatus", summary.transferStatus());
        values.put("wouldMutateTarget", wouldMutateTarget());
        values.put("sourceStoreKind", sourceStoreKind);
        values.put("targetStoreKind", targetStoreKind);
        values.put("dryRun", options.dryRun());
        values.put("overwriteExisting", options.overwriteExisting());
        values.put("verifyAfterCopy", options.verifyAfterCopy());
        values.put("plannedDocumentCount", plannedDocumentCount());
        values.put("copyableDocumentCount", copyableDocumentCount());
        values.put("skippedDocumentCount", skippedDocumentCount());
        values.put("blockedDocumentCount", blockedDocumentCount());
        values.put("issueCount", issueCount());
        values.put("findingCount", findings.size());
        values.put("errorFindingCount", errorFindingCount());
        values.put("warningFindingCount", warningFindingCount());
        values.put("infoFindingCount", infoFindingCount());
        values.put("options", options.toMap());
        values.put("plannedDocuments", plannedDocuments);
        values.put("skippedDocuments", skippedDocuments);
        values.put("blockedDocuments", blockedDocuments);
        values.put("documents", documents().stream()
                .map(AgenticCommerceWayangPersistenceTransferDocumentStatus::toMap)
                .toList());
        values.put("issues", issues);
        values.put("findings", findings.stream()
                .map(AgenticCommerceWayangPersistenceTransferFinding::toMap)
                .toList());
        values.put("findingIndex", findingIndex.toMap());
        values.put("summary", summary.toMap());
        values.put("sourcePersistenceTarget", sourcePersistenceTarget());
        values.put("targetPersistenceTarget", targetPersistenceTarget());
        values.put("persistenceTargetComparison", persistenceTargetComparison().toMap());
        values.put("sourceStatus", sourceStatus);
        values.put("targetStatus", targetStatus);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }

    private static Map<String, Object> planAttributes(
            AgenticCommerceWayangPersistenceTransferReport report,
            AgenticCommerceWayangPersistenceTransferOptions options) {
        Map<String, Object> values = new LinkedHashMap<>(report.attributes());
        values.put("planningOnly", true);
        values.put("dryRun", options.dryRun());
        values.put("overwriteExisting", options.overwriteExisting());
        values.put("verifyAfterCopy", options.verifyAfterCopy());
        values.put("plannedDocumentCount", report.plannedDocumentCount());
        values.put("copyableDocumentCount", report.plannedDocumentCount() - report.blockedDocumentCount());
        values.put("blockedDocumentCount", report.blockedDocumentCount());
        return Map.copyOf(values);
    }
}
