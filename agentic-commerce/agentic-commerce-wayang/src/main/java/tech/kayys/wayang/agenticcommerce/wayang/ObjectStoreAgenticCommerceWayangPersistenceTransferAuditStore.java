package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Object-storage backed transfer audit store for S3/RustFS-compatible clients.
 */
public final class ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore
        implements AgenticCommerceWayangPersistenceTransferAuditSink,
                AgenticCommerceWayangPersistenceTransferAuditReader {

    public static final String STORAGE_KIND =
            AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE;
    public static final String DEFAULT_AUDIT_OBJECT = "persistence-transfer-audit.jsonl";

    private final AgenticCommerceObjectStoreConfig config;
    private final AgenticCommerceObjectStoreClient client;
    private final String auditObject;
    private final AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy;
    private final InMemoryAgenticCommerceWayangPersistenceTransferAuditSink mirror;

    public ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client) {
        this(config, client, DEFAULT_AUDIT_OBJECT,
                InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.DEFAULT_MAX_TRAILS);
    }

    public ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client,
            String auditObject,
            int maxTrails) {
        this(config, client, auditObject,
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(maxTrails));
    }

    public ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client,
            String auditObject,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        this.config = Objects.requireNonNull(config, "config");
        this.client = Objects.requireNonNull(client, "client");
        this.auditObject = normalizeAuditObject(auditObject);
        this.retentionPolicy = retentionPolicy == null
                ? AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.defaults()
                : retentionPolicy;
        this.mirror = new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(this.retentionPolicy.maxTrails());
        reload();
    }

    public static ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore configured(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client,
            String auditObject,
            int maxTrails) {
        return new ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore(
                config,
                client,
                auditObject,
                maxTrails);
    }

    public static ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore configured(
            AgenticCommerceObjectStoreConfig config,
            AgenticCommerceObjectStoreClient client,
            String auditObject,
            AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy retentionPolicy) {
        return new ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore(
                config,
                client,
                auditObject,
                retentionPolicy);
    }

    public AgenticCommerceObjectStoreConfig config() {
        return config;
    }

    public AgenticCommerceObjectStoreClient client() {
        return client;
    }

    public String auditObject() {
        return auditObject;
    }

    public String auditObjectKey() {
        return config.objectKey(auditObject);
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
        return client.readText(config.bucket(), auditObjectKey())
                .map(AgenticCommerceWayangPersistenceTransferAuditJsonl::linesFromBody)
                .orElseGet(List::of);
    }

    private void writeLines(List<String> lines) {
        client.writeText(
                config.bucket(),
                auditObjectKey(),
                AgenticCommerceWayangPersistenceTransferAuditJsonl.MIME_JSONL,
                AgenticCommerceWayangPersistenceTransferAuditJsonl.bodyFromLines(lines));
    }

    private void reloadMirror(List<String> lines) {
        mirror.clear();
        lines.forEach(line -> AgenticCommerceWayangPersistenceTransferAuditJsonl.fromJsonLine(line)
                .ifPresent(mirror::record));
    }

    private static String normalizeAuditObject(String auditObject) {
        String normalized = AgenticCommerceWayangMaps.text(auditObject);
        if (normalized.isBlank()) {
            normalized = DEFAULT_AUDIT_OBJECT;
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? DEFAULT_AUDIT_OBJECT : normalized;
    }
}
