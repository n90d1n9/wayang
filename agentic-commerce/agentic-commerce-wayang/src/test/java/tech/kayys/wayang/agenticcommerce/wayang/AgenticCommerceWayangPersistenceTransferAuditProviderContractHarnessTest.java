package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED;

class AgenticCommerceWayangPersistenceTransferAuditProviderContractHarnessTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void memoryConfigBuiltThroughProvidersPassesContract() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2);

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness.retainedLatestTwo()
                        .run(config);

        assertPassedProviderContract(report, AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY);
        assertThat(report.reloadAttempted()).isFalse();
        assertThat(report.attributes())
                .containsEntry("providerContract", true)
                .containsEntry("providerMatched", true)
                .containsEntry("configuredMaxTrails", 2);
    }

    @Test
    void fileConfigBuiltThroughProvidersPassesReloadContract() throws Exception {
        Path journal = temporaryDirectory.resolve("provider/file/audit.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.file(journal.toString(), 2);

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness.retainedLatestTwo()
                        .run(config, true);

        assertPassedProviderContract(report, AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE);
        assertThat(report.reloadAttempted()).isTrue();
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        assertThat(Files.readAllLines(journal, StandardCharsets.UTF_8)).hasSize(2);
    }

    @Test
    void objectStoreConfigBuiltThroughResolverPassesReloadContract() {
        InMemoryAgenticCommerceObjectStoreClient client =
                InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "rustfs",
                        "bucket",
                        "wayang-audit",
                        "keyPrefix",
                        "provider-contract",
                        "journalObject",
                        "audit.jsonl",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness
                        .retainedLatestTwo(AgenticCommerceObjectStoreClientResolver.fixed(client), null)
                        .run(config, true);

        assertPassedProviderContract(report, AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE);
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        assertThat(config.objectStoreConfig().provider()).isEqualTo(AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS);
        assertThat(client.contains(
                "wayang-audit",
                config.objectStoreConfig().objectKey(config.journalPath()))).isTrue();
    }

    @Test
    void databaseConfigBuiltThroughResolverPassesReloadContract() {
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "jdbc",
                        "table",
                        "wayang_audit",
                        "namespace",
                        "provider-contract",
                        "journalDocument",
                        "audit.jsonl",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness
                        .retainedLatestTwo(null, AgenticCommerceDatabasePersistenceClientResolver.fixed(client))
                        .run(config, true);

        assertPassedProviderContract(report, AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE);
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        assertThat(config.databaseConfig().provider()).isEqualTo(AgenticCommerceDatabasePersistenceConfig.PROVIDER_JDBC);
        assertThat(client.contains(
                "wayang_audit",
                config.databaseConfig().documentKey(config.journalPath()))).isTrue();
    }

    @Test
    void durableFirstCompositeConfigPassesReloadContract() throws Exception {
        Path journal = temporaryDirectory.resolve("provider/composite/audit.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.composite(List.of(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.file(journal.toString(), 2),
                        AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2)));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness.retainedLatestTwo()
                        .run(config, true);

        assertPassedProviderContract(report, AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE);
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        assertThat(Files.readAllLines(journal, StandardCharsets.UTF_8)).hasSize(2);
        assertThat(map(report.attributes().get("config")).get("children")).isInstanceOf(List.class);
    }

    @Test
    void missingObjectStoreResolverReturnsContractIssue() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "s3",
                        "bucket",
                        "wayang-audit",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness.retainedLatestTwo()
                        .run(config, true);

        assertThat(report.passed()).isFalse();
        assertThat(report.issues()).containsExactly(SINK_BUILD_FAILED);
        assertThat(report.attributes())
                .containsEntry("providerMatched", true)
                .containsEntry("providerStorageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE);
    }

    @Test
    void customProviderConfigPassesContractThroughCustomRegistry() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "hosted-audit",
                        "maxTrails",
                        2));
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders providers =
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.builder()
                        .providers(AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults())
                        .provider(new HostedAuditProvider())
                        .build();

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditProviderContractHarness.retainedLatestTwo()
                        .withProviders(providers)
                        .run(config);

        assertPassedProviderContract(report, "hosted-audit");
        assertThat(report.attributes())
                .containsEntry("providerMatched", true)
                .containsEntry("providerStorageKind", "hosted-audit");
    }

    private static void assertPassedProviderContract(
            AgenticCommerceWayangPersistenceTransferAuditContractReport report,
            String storageKind) {
        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.retainedTrailCount()).isEqualTo(2);
        assertThat(report.attributes())
                .containsEntry("providerContract", true)
                .containsEntry("storageKind", storageKind)
                .containsEntry("providerStorageKind", storageKind);
        assertThat(map(report.retainedPage()).get("trailTypes").toString())
                .contains(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
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
            return new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(context.config().maxTrails());
        }
    }
}
