package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceWayangPersistenceTransferAuditConfigTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultsBuildReadableInMemorySink() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.defaults();
        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink();

        assertThat(config.storageKind())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY);
        assertThat(config.memoryStore()).isTrue();
        assertThat(sink).isInstanceOf(InMemoryAgenticCommerceWayangPersistenceTransferAuditSink.class);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).isEmpty();
        assertThat(config.toMap())
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY)
                .containsEntry("readable", true);
    }

    @Test
    void fileConfigBindsAliasesAndRetainsJsonl() throws Exception {
        Path auditDirectory = temporaryDirectory.resolve("audit");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "type",
                        "local-file",
                        "directory",
                        auditDirectory.toString(),
                        "journalFile",
                        "transfer.jsonl",
                        "maxTrails",
                        "2"));
        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink();
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        sink.record(first);
        sink.record(second);
        sink.record(third);

        assertThat(config.storageKind()).isEqualTo(AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE);
        assertThat(config.fileStore()).isTrue();
        assertThat(config.maxTrails()).isEqualTo(2);
        assertThat(config.journalPath()).isEqualTo(auditDirectory.resolve("transfer.jsonl").toString());
        assertThat(sink).isInstanceOf(FileSystemAgenticCommerceWayangPersistenceTransferAuditStore.class);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).containsExactly(second, third);
        List<String> retainedLines = Files.readAllLines(Path.of(config.journalPath()));
        assertThat(retainedLines).hasSize(2);
        assertThat(String.join("\n", retainedLines))
                .doesNotContain("\"trailStatus\":\"ready\"")
                .contains("\"trailStatus\":\"complete\"", "\"trailStatus\":\"applied\"");
        assertThat(config.toMap())
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE)
                .containsEntry("fileBacked", true)
                .containsEntry("maxTrails", 2);
    }

    @Test
    void fileConfigPreservesRetentionPolicyAndPrunesByBytes() throws Exception {
        Path journal = temporaryDirectory.resolve("audit/byte-retained.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "type",
                        "file",
                        "journalPath",
                        journal.toString(),
                        "retentionPolicy",
                        Map.of(
                                "maxTrails",
                                10,
                                "maxBytes",
                                "1B")));
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);

        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink();
        sink.record(first);
        sink.record(second);

        assertThat(config.maxTrails()).isEqualTo(10);
        assertThat(config.retentionPolicy().maxBytes()).isEqualTo(1L);
        assertThat(config.retentionPolicy().byteLimited()).isTrue();
        assertThat(sink).isInstanceOf(FileSystemAgenticCommerceWayangPersistenceTransferAuditStore.class);
        FileSystemAgenticCommerceWayangPersistenceTransferAuditStore store =
                (FileSystemAgenticCommerceWayangPersistenceTransferAuditStore) sink;
        assertThat(store.retentionPolicy().maxBytes()).isEqualTo(1L);
        assertThat(Files.readAllLines(journal)).hasSize(1);
        assertThat(String.join("\n", Files.readAllLines(journal)))
                .doesNotContain("\"trailStatus\":\"ready\"")
                .contains("\"trailStatus\":\"complete\"");
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).containsExactly(second);
        assertThat(map(config.toMap().get("retentionPolicy")))
                .containsEntry("maxTrails", 10)
                .containsEntry("maxBytes", 1L)
                .containsEntry("byteLimited", true);
    }

    @Test
    void compositeConfigBuildsFanOutSinkFromChildren() {
        Path journal = temporaryDirectory.resolve("audit/composite.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "fan-out",
                        "sinks",
                        List.of(
                                Map.of(
                                        "storageKind",
                                        "memory",
                                        "maxTrails",
                                        2),
                                Map.of(
                                        "storageKind",
                                        "file",
                                        "journalPath",
                                        journal.toString(),
                                        "maxTrails",
                                        2))));
        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink();
        AgenticCommerceWayangPersistenceTransferAuditTrail trail =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);

        sink.record(trail);

        assertThat(config.compositeStore()).isTrue();
        assertThat(config.children()).hasSize(2);
        assertThat(sink).isInstanceOf(CompositeAgenticCommerceWayangPersistenceTransferAuditSink.class);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10))
                .trails()).containsExactly(trail);
        assertThat(Files.exists(journal)).isTrue();
        assertThat(config.toMap().get("children")).isInstanceOf(List.class);
        assertThat(config.toMap()).containsEntry("composite", true);
    }

    @Test
    void noopAliasesBuildEmptyReader() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "mode",
                        "disabled"));
        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink();

        sink.record(trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true));

        assertThat(config.noopStore()).isTrue();
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).isEmpty();
    }

    @Test
    void defaultProvidersExposeBuiltInAuditStores() {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers =
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults();

        assertThat(providers.storageKinds())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_NOOP,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE);
        assertThat(providers.toMap())
                .containsEntry("providerCount", 6)
                .containsKey("providers");
    }

    @Test
    void schemaDescribesConfigValidationRemediationAndProviderSurfaces() {
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers =
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.builder()
                        .providers(AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults())
                        .provider(new HostedAuditProvider())
                        .build();

        Map<String, Object> schema = AgenticCommerceWayangPersistenceTransferAuditConfig.schema(providers).toMap();
        Map<String, Object> storage = map(schema.get("storage"));
        Map<String, Object> retentionPolicy = map(schema.get("retentionPolicy"));
        Map<String, Object> validation = map(schema.get("validation"));
        Map<String, Object> remediation = map(schema.get("remediation"));
        Map<String, Object> patch = map(remediation.get("patch"));

        assertThat(schema)
                .containsEntry(
                        "schemaId",
                        AgenticCommerceWayangPersistenceTransferAuditConfigSchema.SCHEMA_ID)
                .containsEntry("schemaVersion", 1);
        assertThat(strings(storage.get("storageKinds")))
                .contains(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE,
                        AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE,
                        "hosted-audit");
        assertThat(strings(retentionPolicy.get("countAliases"))).contains("maxTrails", "maxEvents", "capacity");
        assertThat(strings(retentionPolicy.get("byteAliases"))).contains("maxBytes", "byteLimit");
        assertThat(retentionPolicy)
                .containsEntry("unlimitedBytes", AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy
                        .UNLIMITED_BYTES);
        assertThat(strings(validation.get("configIssueCodes")))
                .contains(AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues
                        .AUDIT_RETENTION_BELOW_CONTRACT);
        assertThat(strings(validation.get("contractIssueCodes")))
                .contains(AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED);
        assertThat(strings(remediation.get("operations")))
                .contains(AgenticCommerceWayangPersistenceTransferAuditConfigRemediation.OPERATION_INCREASE_BYTE_LIMIT);
        assertThat(strings(patch.get("operations")))
                .contains(AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE);
        assertThat(patch)
                .containsEntry("pathSyntax", "$.field[.nestedField]")
                .containsEntry("application", "copy-on-write");
        assertThat(strings(remediation.get("patchApplicationReportFields")))
                .contains("patchable", "resolved", "before", "after", "patchedConfig");
        assertThat(strings(remediation.get("patchApplicationSummaryFields")))
                .contains("patchable", "resolved", "beforeErrorCodes", "afterErrorCodes");
        assertThat(map(schema.get("providerRegistry"))).containsEntry("providerCount", 7);
    }

    @Test
    void objectStoreConfigRequiresClientAndPersistsRetainedJsonl() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "s3",
                        "bucket",
                        "wayang-audit",
                        "keyPrefix",
                        "prod/agentic-commerce",
                        "journalObject",
                        "transfer-audit.jsonl",
                        "maxTrails",
                        2));
        InMemoryAgenticCommerceObjectStoreClient client =
                InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        assertThat(config.storageKind())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE);
        assertThat(config.objectStoreBacked()).isTrue();
        assertThat(config.objectStoreConfig().provider()).isEqualTo(AgenticCommerceObjectStoreConfig.PROVIDER_S3);
        assertThat(config.objectStoreConfig().bucket()).isEqualTo("wayang-audit");
        assertThat(config.journalPath()).isEqualTo("transfer-audit.jsonl");
        assertThatThrownBy(config::buildSink)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an AgenticCommerceObjectStoreClient");

        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink(client);
        sink.record(first);
        sink.record(second);
        sink.record(third);

        assertThat(sink).isInstanceOf(ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.class);
        ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore objectStore =
                (ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore) sink;
        assertThat(objectStore.auditObjectKey()).isEqualTo("prod/agentic-commerce/transfer-audit.jsonl");
        assertThat(client.contains("wayang-audit", objectStore.auditObjectKey())).isTrue();
        String body = client.readText("wayang-audit", objectStore.auditObjectKey()).orElseThrow();
        assertThat(AgenticCommerceWayangPersistenceTransferAuditJsonl.linesFromBody(body)).hasSize(2);
        assertThat(body)
                .doesNotContain("\"trailStatus\":\"ready\"")
                .contains("\"trailStatus\":\"complete\"", "\"trailStatus\":\"applied\"");

        AgenticCommerceWayangPersistenceTransferAuditSink reloaded = config.buildSink(client);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(reloaded)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).containsExactly(second, third);
    }

    @Test
    void databaseConfigRequiresClientAndPersistsRetainedJsonl() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "postgres",
                        "table",
                        "wayang_audit",
                        "namespace",
                        "prod/agentic-commerce",
                        "journalDocument",
                        "transfer-audit.jsonl",
                        "maxTrails",
                        2));
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        assertThat(config.storageKind())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE);
        assertThat(config.databaseBacked()).isTrue();
        assertThat(config.databaseConfig().provider())
                .isEqualTo(AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES);
        assertThat(config.databaseConfig().tableName()).isEqualTo("wayang_audit");
        assertThat(config.databaseConfig().namespace()).isEqualTo("prod/agentic-commerce");
        assertThat(config.journalPath()).isEqualTo("transfer-audit.jsonl");
        assertThatThrownBy(config::buildSink)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires an AgenticCommerceDatabasePersistenceClient");

        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink(client);
        sink.record(first);
        sink.record(second);
        sink.record(third);

        assertThat(sink).isInstanceOf(DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.class);
        DatabaseAgenticCommerceWayangPersistenceTransferAuditStore database =
                (DatabaseAgenticCommerceWayangPersistenceTransferAuditStore) sink;
        assertThat(database.auditDocumentKey()).isEqualTo("prod/agentic-commerce/transfer-audit.jsonl");
        assertThat(client.contains("wayang_audit", database.auditDocumentKey())).isTrue();
        String body = client.readText("wayang_audit", database.auditDocumentKey()).orElseThrow();
        assertThat(AgenticCommerceWayangPersistenceTransferAuditJsonl.linesFromBody(body)).hasSize(2);
        assertThat(body)
                .doesNotContain("\"trailStatus\":\"ready\"")
                .contains("\"trailStatus\":\"complete\"", "\"trailStatus\":\"applied\"");

        AgenticCommerceWayangPersistenceTransferAuditSink reloaded = config.buildSink(client);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(reloaded)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10))
                .trails()).containsExactly(second, third);
    }

    @Test
    void customProviderCanBuildFutureAuditBackend() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "hosted-audit"));
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers =
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.builder()
                        .providers(AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults())
                        .provider(new HostedAuditProvider())
                        .build();
        AgenticCommerceWayangPersistenceTransferAuditTrail trail =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);

        AgenticCommerceWayangPersistenceTransferAuditSink sink = config.buildSink(providers);
        sink.record(trail);

        assertThat(providers.storageKinds()).contains("hosted-audit");
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10))
                .trails()).containsExactly(trail);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditTrail trail(
            String eventType,
            String eventStatus,
            boolean successful) {
        AgenticCommerceWayangPersistenceTransferAuditEvent event =
                new AgenticCommerceWayangPersistenceTransferAuditEvent(
                        eventType,
                        eventStatus,
                        successful,
                        false,
                        false,
                        successful,
                        false,
                        0,
                        0,
                        0,
                        1,
                        1,
                        successful ? 1 : 0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        Map.of("eventStatus", eventStatus));
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                eventType,
                List.of(event),
                Map.of("eventStatus", eventStatus));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }

    private static final class HostedAuditProvider
            implements AgenticCommerceWayangPersistenceTransferAuditStoreProvider {

        @Override
        public String storageKind() {
            return "hosted-audit";
        }

        @Override
        public boolean supports(AgenticCommerceWayangPersistenceTransferAuditConfig config) {
            return "hosted-audit".equals(config.storageKind());
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditSink build(
                AgenticCommerceWayangPersistenceTransferAuditProviderContext context) {
            return new HostedAuditSink();
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of("storageKind", storageKind(), "custom", true);
        }
    }

    private static final class HostedAuditSink
            implements AgenticCommerceWayangPersistenceTransferAuditSink,
                    AgenticCommerceWayangPersistenceTransferAuditReader {

        private final InMemoryAgenticCommerceWayangPersistenceTransferAuditSink delegate =
                new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink();

        @Override
        public void record(AgenticCommerceWayangPersistenceTransferAuditTrail trail) {
            delegate.record(trail);
        }

        @Override
        public AgenticCommerceWayangPersistenceTransferAuditPage query(
                AgenticCommerceWayangPersistenceTransferAuditQuery query) {
            return delegate.query(query);
        }
    }
}
