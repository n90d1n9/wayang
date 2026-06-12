package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Database-backed transfer audit store for document-style database clients.
 */
public final class DatabaseAgenticCommerceWayangPersistenceTransferAuditStore
        implements AgenticCommerceWayangPersistenceTransferAuditSink,
                AgenticCommerceWayangPersistenceTransferAuditReader {

    public static final String STORAGE_KIND =
            AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE;
    public static final String DEFAULT_AUDIT_DOCUMENT = "persistence-transfer-audit.jsonl";

    private final AgenticCommerceDatabasePersistenceConfig config;
    private final AgenticCommerceDatabasePersistenceClient client;
    private final String auditDocument;
    private final AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy;
    private final InMemoryAgenticCommerceWayangPersistenceTransferAuditSink mirror;

    public DatabaseAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client) {
        this(config, client, DEFAULT_AUDIT_DOCUMENT,
                InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS);
    }

    public DatabaseAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client,
            String auditDocument,
            int maxTrails) {
        this(config, client, auditDocument,
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(maxTrails));
    }

    public DatabaseAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client,
            String auditDocument,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        this.config = config == null ? AgenticCommerceDatabasePersistenceConfig.defaults() : config;
        this.client = Objects.requireNonNull(client, "client");
        this.auditDocument = normalizeAuditDocument(auditDocument);
        this.retentionPolicy = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.defaults()
                : retentionPolicy;
        this.mirror = new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(this.retentionPolicy.maxTrails());
        reload();
    }

    public static DatabaseAgenticCommerceWayangPersistenceTransferAuditStore configured(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client,
            String auditDocument,
            int maxTrails) {
        return new DatabaseAgenticCommerceWayangPersistenceTransferAuditStore(
                config,
                client,
                auditDocument,
                maxTrails);
    }

    public static DatabaseAgenticCommerceWayangPersistenceTransferAuditStore configured(
            AgenticCommerceDatabasePersistenceConfig config,
            AgenticCommerceDatabasePersistenceClient client,
            String auditDocument,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        return new DatabaseAgenticCommerceWayangPersistenceTransferAuditStore(
                config,
                client,
                auditDocument,
                retentionPolicy);
    }

    public AgenticCommerceDatabasePersistenceConfig config() {
        return config;
    }

    public AgenticCommerceDatabasePersistenceClient client() {
        return client;
    }

    public String auditDocument() {
        return auditDocument;
    }

    public String auditDocumentKey() {
        return config.documentKey(auditDocument);
    }

    public int maxTrails() {
        return retentionPolicy.maxTrails();
    }

    public AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy() {
        return retentionPolicy;
    }

    @Override
    public synchronized void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
        if (trail == null) {
            return;
        }
        List<String> lines = new ArrayList<>(journalLines());
        lines.add(AgenticCommerceWayangPersistenceTransferAuditJsonl.toJsonLine(trail));
        List<String> retained = AgenticCommerceWayangPersistenceTransferAuditJsonl.retainedLines(
                lines,
                retentionPolicy);
        writeLines(retained);
        reloadMirror(retained);
    }

    @Override
    public synchronized AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditQuery query) {
        return mirror.query(query);
    }

    public synchronized List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails() {
        return mirror.trails();
    }

    public synchronized Optional<AgenticCommerceWayangPersistenceTransferAuditTrail> latest() {
        return mirror.latest();
    }

    public synchronized void reload() {
        mirror.clear();
        List<String> originalLines = journalLines();
        List<String> lines = AgenticCommerceWayangPersistenceTransferAuditJsonl.retainedLines(
                originalLines,
                retentionPolicy);
        reloadMirror(lines);
        if (!originalLines.equals(lines)) {
            writeLines(lines);
        }
    }

    private List<String> journalLines() {
        return client.readText(config.tableName(), auditDocumentKey())
                .map(AgenticCommerceWayangPersistenceTransferAuditJsonl::linesFromBody)
                .orElseGet(List::of);
    }

    private void writeLines(List<String> lines) {
        client.writeText(
                config.tableName(),
                auditDocumentKey(),
                AgenticCommerceWayangPersistenceTransferAuditJsonl.MIME_JSONL,
                AgenticCommerceWayangPersistenceTransferAuditJsonl.bodyFromLines(lines));
    }

    private void reloadMirror(List<String> lines) {
        mirror.clear();
        lines.forEach(line -> AgenticCommerceWayangPersistenceTransferAuditJsonl.fromJsonLine(line)
                .ifPresent(mirror::record));
    }

    private static String normalizeAuditDocument(String auditDocument) {
        String normalized = AgenticCommerceWayangMaps.text(auditDocument);
        if (normalized.isBlank()) {
            normalized = DEFAULT_AUDIT_DOCUMENT;
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? DEFAULT_AUDIT_DOCUMENT : normalized;
    }
}
