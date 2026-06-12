package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Per-document transfer action derived from a persistence transfer report or plan.
 */
public record AgenticCommerceWayangPersistenceTransferDocumentStatus(
        AgenticCommerceWayangPersistenceDocument document,
        boolean planned,
        boolean copied,
        boolean skipped,
        boolean blocked,
        boolean copyable,
        boolean dryRun,
        boolean planningOnly,
        List<String> issues) {

    public AgenticCommerceWayangPersistenceTransferDocumentStatus {
        document = Objects.requireNonNull(document, "document");
        issues = AgenticCommerceWayangMaps.stringList(issues);
    }

    public static AgenticCommerceWayangPersistenceTransferDocumentStatus from(
            AgenticCommerceWayangPersistenceDocument document,
            List<String> plannedDocuments,
            List<String> copiedDocuments,
            List<String> skippedDocuments,
            List<String> blockedDocuments,
            List<String> issues,
            boolean dryRun,
            boolean planningOnly) {
        boolean planned = contains(plannedDocuments, document);
        boolean copied = contains(copiedDocuments, document);
        boolean skipped = contains(skippedDocuments, document);
        boolean blocked = contains(blockedDocuments, document);
        return new AgenticCommerceWayangPersistenceTransferDocumentStatus(
                document,
                planned,
                copied,
                skipped,
                blocked,
                planned && !blocked && !skipped,
                dryRun,
                planningOnly,
                documentIssues(document, issues));
    }

    public String id() {
        return document.id();
    }

    public String fileName() {
        return document.fileName();
    }

    public String action() {
        if (copied) {
            return "copied";
        }
        if (blocked) {
            return "blocked";
        }
        if (skipped) {
            return "skipped";
        }
        if (planned && (planningOnly || dryRun)) {
            return "would_copy";
        }
        if (planned) {
            return "failed";
        }
        return "not_selected";
    }

    public boolean failed() {
        return "failed".equals(action());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id());
        values.put("fileName", fileName());
        values.put("action", action());
        values.put("planned", planned);
        values.put("copied", copied);
        values.put("skipped", skipped);
        values.put("blocked", blocked);
        values.put("copyable", copyable);
        values.put("dryRun", dryRun);
        values.put("planningOnly", planningOnly);
        values.put("failed", failed());
        values.put("issueCount", issues.size());
        values.put("issues", issues);
        values.put("document", document.toMap());
        return Map.copyOf(values);
    }

    private static boolean contains(List<String> values, AgenticCommerceWayangPersistenceDocument document) {
        return AgenticCommerceWayangMaps.stringList(values).contains(document.id());
    }

    private static List<String> documentIssues(
            AgenticCommerceWayangPersistenceDocument document,
            List<String> issues) {
        String prefix = issuePrefix(document);
        return AgenticCommerceWayangMaps.stringList(issues).stream()
                .filter(issue -> issue.startsWith(prefix))
                .toList();
    }

    private static String issuePrefix(AgenticCommerceWayangPersistenceDocument document) {
        if (AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG.equals(document.id())) {
            return "runtime_config_";
        }
        if (AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_CONFIG.equals(document.id())) {
            return "bootstrap_config_";
        }
        if (AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_REPORT.equals(document.id())) {
            return "bootstrap_report_";
        }
        if (AgenticCommerceWayangPersistenceTransfer.DOCUMENT_MANIFEST.equals(document.id())) {
            return "manifest_";
        }
        return document.id() + "_";
    }
}
