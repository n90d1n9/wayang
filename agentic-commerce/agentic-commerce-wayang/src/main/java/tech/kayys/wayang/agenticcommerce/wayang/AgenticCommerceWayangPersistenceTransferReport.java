package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-ready report for copying persisted Agentic Commerce Wayang state.
 */
public record AgenticCommerceWayangPersistenceTransferReport(
        String sourceStoreKind,
        String targetStoreKind,
        boolean runtimeConfigCopied,
        boolean bootstrapConfigCopied,
        boolean bootstrapReportCopied,
        boolean manifestCopied,
        AgenticCommerceWayangPersistenceTransferOptions options,
        List<String> plannedDocuments,
        List<String> skippedDocuments,
        List<String> blockedDocuments,
        List<String> issues,
        Map<String, Object> sourceStatus,
        Map<String, Object> targetStatusBefore,
        Map<String, Object> targetStatusAfter,
        Map<String, Object> attributes) {

    public AgenticCommerceWayangPersistenceTransferReport {
        sourceStoreKind = AgenticCommerceWayangMaps.text(sourceStoreKind);
        targetStoreKind = AgenticCommerceWayangMaps.text(targetStoreKind);
        options = options == null ? AgenticCommerceWayangPersistenceTransferOptions.defaults() : options;
        plannedDocuments = AgenticCommerceWayangMaps.stringList(plannedDocuments);
        skippedDocuments = AgenticCommerceWayangMaps.stringList(skippedDocuments);
        blockedDocuments = AgenticCommerceWayangMaps.stringList(blockedDocuments);
        issues = AgenticCommerceWayangMaps.stringList(issues);
        sourceStatus = AgenticCommerceWayangMaps.copy(sourceStatus);
        targetStatusBefore = AgenticCommerceWayangMaps.copy(targetStatusBefore);
        targetStatusAfter = AgenticCommerceWayangMaps.copy(targetStatusAfter);
        attributes = AgenticCommerceWayangMaps.copy(attributes);
    }

    public boolean passed() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int copiedDocumentCount() {
        int count = 0;
        count += runtimeConfigCopied ? 1 : 0;
        count += bootstrapConfigCopied ? 1 : 0;
        count += bootstrapReportCopied ? 1 : 0;
        count += manifestCopied ? 1 : 0;
        return count;
    }

    public int skippedDocumentCount() {
        return skippedDocuments.size();
    }

    public int plannedDocumentCount() {
        return plannedDocuments.size();
    }

    public int blockedDocumentCount() {
        return blockedDocuments.size();
    }

    public List<String> copiedDocuments() {
        List<String> values = new java.util.ArrayList<>();
        if (runtimeConfigCopied) {
            values.add(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        }
        if (bootstrapConfigCopied) {
            values.add(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_CONFIG);
        }
        if (bootstrapReportCopied) {
            values.add(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_REPORT);
        }
        if (manifestCopied) {
            values.add(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_MANIFEST);
        }
        return List.copyOf(values);
    }

    public List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents() {
        return AgenticCommerceWayangPersistenceDocuments.ALL.stream()
                .map(document -> AgenticCommerceWayangPersistenceTransferDocumentStatus.from(
                        document,
                        plannedDocuments,
                        copiedDocuments(),
                        skippedDocuments,
                        blockedDocuments,
                        issues,
                        options.dryRun(),
                        false))
                .toList();
    }

    public Map<String, Object> sourcePersistenceTarget() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(sourceStatus, sourceStoreKind);
    }

    public Map<String, Object> targetPersistenceTargetBefore() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(
                targetStatusBefore,
                targetStoreKind);
    }

    public Map<String, Object> targetPersistenceTargetAfter() {
        return AgenticCommerceWayangPersistenceTargetDescriptor.mapFromStatus(
                targetStatusAfter,
                targetStoreKind);
    }

    public AgenticCommerceWayangPersistenceTargetComparison persistenceTargetComparisonBefore() {
        return AgenticCommerceWayangPersistenceTargetComparison.between(
                "source",
                sourcePersistenceTarget(),
                "targetBefore",
                targetPersistenceTargetBefore());
    }

    public AgenticCommerceWayangPersistenceTargetComparison persistenceTargetComparisonAfter() {
        return AgenticCommerceWayangPersistenceTargetComparison.between(
                "source",
                sourcePersistenceTarget(),
                "targetAfter",
                targetPersistenceTargetAfter());
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

    public AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent() {
        return AgenticCommerceWayangPersistenceTransferAuditEvent.from(this);
    }

    public AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail() {
        return AgenticCommerceWayangPersistenceTransferAuditTrail.from(this);
    }

    public Map<String, Object> toMap() {
        AgenticCommerceWayangPersistenceTransferSummary summary = summary();
        List<AgenticCommerceWayangPersistenceTransferFinding> findings = findings();
        AgenticCommerceWayangPersistenceTransferFindingIndex findingIndex =
                AgenticCommerceWayangPersistenceTransferFindingIndex.from(findings);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed());
        values.put("transferStatus", summary.transferStatus());
        values.put("sourceStoreKind", sourceStoreKind);
        values.put("targetStoreKind", targetStoreKind);
        values.put("dryRun", options.dryRun());
        values.put("overwriteExisting", options.overwriteExisting());
        values.put("verifyAfterCopy", options.verifyAfterCopy());
        values.put("plannedDocumentCount", plannedDocumentCount());
        values.put("copiedDocumentCount", copiedDocumentCount());
        values.put("skippedDocumentCount", skippedDocumentCount());
        values.put("blockedDocumentCount", blockedDocumentCount());
        values.put("issueCount", issueCount());
        values.put("findingCount", findings.size());
        values.put("errorFindingCount", errorFindingCount());
        values.put("warningFindingCount", warningFindingCount());
        values.put("infoFindingCount", infoFindingCount());
        values.put("runtimeConfigCopied", runtimeConfigCopied);
        values.put("bootstrapConfigCopied", bootstrapConfigCopied);
        values.put("bootstrapReportCopied", bootstrapReportCopied);
        values.put("manifestCopied", manifestCopied);
        values.put("options", options.toMap());
        values.put("plannedDocuments", plannedDocuments);
        values.put("skippedDocuments", skippedDocuments);
        values.put("blockedDocuments", blockedDocuments);
        values.put("copiedDocuments", copiedDocuments());
        values.put("documents", documents().stream()
                .map(AgenticCommerceWayangPersistenceTransferDocumentStatus::toMap)
                .toList());
        values.put("issues", issues);
        values.put("findings", findings.stream()
                .map(AgenticCommerceWayangPersistenceTransferFinding::toMap)
                .toList());
        values.put("findingIndex", findingIndex.toMap());
        values.put("summary", summary.toMap());
        values.put("auditEvent", auditEvent().toMap());
        values.put("auditTrail", auditTrail().toMap());
        values.put("sourcePersistenceTarget", sourcePersistenceTarget());
        values.put("targetPersistenceTargetBefore", targetPersistenceTargetBefore());
        values.put("targetPersistenceTargetAfter", targetPersistenceTargetAfter());
        values.put("persistenceTargetComparisonBefore", persistenceTargetComparisonBefore().toMap());
        values.put("persistenceTargetComparisonAfter", persistenceTargetComparisonAfter().toMap());
        values.put("sourceStatus", sourceStatus);
        values.put("targetStatusBefore", targetStatusBefore);
        values.put("targetStatusAfter", targetStatusAfter);
        values.put("attributes", attributes);
        return Map.copyOf(values);
    }
}
