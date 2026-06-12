package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_COUNT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditDiagnosticsIssues.AUDIT_CONTRACT_NOT_RUN;

class AgenticCommerceWayangPersistenceTransferAuditDiagnosticsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void memoryDiagnosticsWarnAboutEphemeralStorageWithoutContract() {
        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2));

        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> storage = map(values.get("storage"));
        Map<String, Object> target = map(values.get("target"));
        Map<String, Object> provider = map(values.get("provider"));
        List<Object> recommendations = list(values.get("recommendations"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.contractAvailable()).isFalse();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_DEGRADED);
        assertThat(diagnostics.warnings())
                .containsExactly(AUDIT_STORAGE_EPHEMERAL, AUDIT_CONTRACT_NOT_RUN);
        assertThat(storage)
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY)
                .containsEntry("ephemeral", true)
                .containsEntry("durable", false);
        assertThat(target)
                .containsEntry("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_IN_MEMORY)
                .containsEntry("durable", false);
        assertThat(provider).containsEntry("providerContract", false);
        assertThat(diagnostics.recommendationCount()).isEqualTo(2);
        assertThat(diagnostics.blockingRecommendationCount()).isZero();
        assertThat(diagnostics.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation
                                .ACTION_CONFIGURE_DURABLE_AUDIT_STORAGE,
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_RUN_AUDIT_CONTRACT);
        assertThat(map(recommendations.get(0)))
                .containsEntry(
                        "action",
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation
                                .ACTION_CONFIGURE_DURABLE_AUDIT_STORAGE)
                .containsEntry("blocking", false);
        assertThat(values).doesNotContainKey("contract");
    }

    @Test
    void memoryDiagnosticsExposeSharedReadinessWithWarnings() {
        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2));

        Map<String, Object> standard = diagnostics.standardReadiness().toMap();
        Map<String, Object> attributes = map(standard.get("attributes"));
        List<Object> probes = list(standard.get("probes"));

        assertThat(standard)
                .containsEntry(
                        "readinessId",
                        AgenticCommerceWayangPersistenceTransferAuditDiagnostics.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0);
        assertThat(list(standard.get("issues"))).isEmpty();
        assertThat(attributes)
                .containsEntry(
                        "diagnosticStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_DEGRADED)
                .containsEntry("auditEnabled", true)
                .containsEntry("durableAuditStorage", false)
                .containsEntry("warningCount", 2)
                .containsEntry("recommendationCount", 2)
                .containsEntry("blockingRecommendationCount", 0);
        assertThat(map(probes.get(0)))
                .containsEntry("probe", "auditConfig")
                .containsEntry("required", true)
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
        assertThat(map(map(probes.get(1)).get("attributes")))
                .containsEntry("auditEnabled", true)
                .containsEntry("durableAuditStorage", false);
        assertThat(map(probes.get(2)))
                .containsEntry("probe", "auditContract")
                .containsEntry("required", false)
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
    }

    @Test
    void noopDiagnosticsIsDisabledButReady() {
        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.noop());

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.auditEnabled()).isFalse();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_DISABLED);
        assertThat(diagnostics.warnings())
                .containsExactly(AUDIT_STORAGE_DISABLED, AUDIT_CONTRACT_NOT_RUN);
        assertThat(diagnostics.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_ENABLE_AUDIT_STORAGE);
        assertThat(diagnostics.blockingRecommendationCount()).isZero();
        assertThat(map(diagnostics.toMap().get("storage")))
                .containsEntry("readable", false)
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_NOOP);
    }

    @Test
    void fileDiagnosticsRunsProviderContractAndSummarizesReload() throws Exception {
        Path journal = temporaryDirectory.resolve("diagnostics/file/audit.jsonl");
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.file(journal.toString(), 2);

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.check(config, true);
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> target = map(values.get("target"));
        Map<String, Object> provider = map(values.get("provider"));
        Map<String, Object> contract = map(values.get("contract"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_HEALTHY);
        assertThat(diagnostics.warningCount()).isZero();
        assertThat(diagnostics.recommendationCount()).isZero();
        assertThat(values)
                .containsEntry("recommendationCount", 0)
                .containsEntry("blockingRecommendationCount", 0);
        assertThat(target)
                .containsEntry("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE)
                .containsEntry("journalPath", journal.toString())
                .containsEntry("durable", true);
        assertThat(provider)
                .containsEntry("providerContract", true)
                .containsEntry("providerMatched", true)
                .containsEntry("verifyReload", true);
        assertThat(contract)
                .containsEntry("passed", true)
                .containsEntry("retainedTrailCount", 2)
                .containsEntry("reloadAttempted", true)
                .containsEntry("reloadTrailCount", 2);
        assertThat(strings(contract.get("trailTypes")))
                .contains(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(Files.readAllLines(journal, StandardCharsets.UTF_8)).hasSize(2);

        Map<String, Object> standard = diagnostics.standardReadiness().toMap();
        Map<String, Object> standardAttributes = map(standard.get("attributes"));
        Map<String, Object> standardContractProbe = map(list(standard.get("probes")).get(2));
        assertThat(standard)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0);
        assertThat(standardAttributes)
                .containsEntry(
                        "diagnosticStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_HEALTHY)
                .containsEntry("durableAuditStorage", true)
                .containsEntry("warningCount", 0)
                .containsEntry("recommendationCount", 0);
        assertThat(standardContractProbe)
                .containsEntry("probe", "auditContract")
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0);
    }

    @Test
    void objectStoreDiagnosticsUsesResolverAndReportsCloudTarget() {
        InMemoryAgenticCommerceObjectStoreClient client =
                InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "rustfs",
                        "bucket",
                        "wayang-audit",
                        "keyPrefix",
                        "diagnostics",
                        "journalObject",
                        "audit.jsonl",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.check(
                        config,
                        AgenticCommerceObjectStoreClientResolver.fixed(client),
                        null,
                        true);
        Map<String, Object> target = map(diagnostics.toMap().get("target"));
        Map<String, Object> contract = map(diagnostics.toMap().get("contract"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_HEALTHY);
        assertThat(target)
                .containsEntry("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE)
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_RUSTFS)
                .containsEntry("bucket", "wayang-audit")
                .containsEntry("auditObjectKey", "diagnostics/audit.jsonl")
                .containsEntry("cloudStorage", true);
        assertThat(contract).containsEntry("passed", true);
        assertThat(client.contains("wayang-audit", "diagnostics/audit.jsonl")).isTrue();
    }

    @Test
    void databaseDiagnosticsUsesResolverAndReportsDatabaseTarget() {
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "postgres",
                        "table",
                        "wayang_audit",
                        "namespace",
                        "diagnostics",
                        "journalDocument",
                        "audit.jsonl",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.check(
                        config,
                        null,
                        AgenticCommerceDatabasePersistenceClientResolver.fixed(client),
                        true);
        Map<String, Object> target = map(diagnostics.toMap().get("target"));
        Map<String, Object> contract = map(diagnostics.toMap().get("contract"));

        assertThat(diagnostics.ready()).isTrue();
        assertThat(target)
                .containsEntry("targetKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_DATABASE)
                .containsEntry("provider", AgenticCommerceDatabasePersistenceConfig.PROVIDER_POSTGRES)
                .containsEntry("tableName", "wayang_audit")
                .containsEntry("auditDocumentKey", "diagnostics/audit.jsonl")
                .containsEntry("databaseStorage", true);
        assertThat(contract)
                .containsEntry("passed", true)
                .containsEntry("reloadTrailCount", 2);
        assertThat(client.contains("wayang_audit", "diagnostics/audit.jsonl")).isTrue();
    }

    @Test
    void missingResolverDiagnosticsAreUnavailableWithContractIssue() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "s3",
                        "bucket",
                        "wayang-audit",
                        "maxTrails",
                        2));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.check(config, true);
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> contract = map(values.get("contract"));
        Map<String, Object> provider = map(values.get("provider"));
        List<Object> recommendations = list(values.get("recommendations"));

        assertThat(diagnostics.ready()).isFalse();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_UNAVAILABLE);
        assertThat(diagnostics.issueCount()).isEqualTo(1);
        assertThat(diagnostics.issues()).containsExactly(SINK_BUILD_FAILED);
        assertThat(provider)
                .containsEntry("providerContract", true)
                .containsEntry("providerMatched", true)
                .containsEntry("providerStorageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_OBJECT_STORE);
        assertThat(contract)
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1);
        assertThat(strings(contract.get("issues"))).containsExactly(SINK_BUILD_FAILED);
        assertThat(diagnostics.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_RESOLVE_AUDIT_PROVIDER);
        assertThat(diagnostics.blockingRecommendationCount()).isEqualTo(1);
        assertThat(map(recommendations.get(0)))
                .containsEntry(
                        "action",
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_RESOLVE_AUDIT_PROVIDER)
                .containsEntry("priority", AgenticCommerceWayangPersistenceTransferAuditRecommendation.PRIORITY_PRIMARY)
                .containsEntry("blocking", true);

        Map<String, Object> standard = diagnostics.standardReadiness().toMap();
        List<Object> standardIssues = list(standard.get("issues"));
        Map<String, Object> standardContractProbe = map(list(standard.get("probes")).get(2));
        assertThat(standard)
                .containsEntry("ready", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", 1);
        assertThat(map(standardIssues.get(0)))
                .containsEntry("code", SINK_BUILD_FAILED)
                .containsEntry("source", "contract");
        assertThat(standardContractProbe)
                .containsEntry("probe", "auditContract")
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1);
    }

    @Test
    void invalidAuditConfigIsUnavailableBeforeContractRun() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.file("diagnostics/audit.jsonl", 1);

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(config);
        Map<String, Object> values = diagnostics.toMap();
        Map<String, Object> validation = map(values.get("validation"));
        List<Object> recommendations = list(values.get("recommendations"));

        assertThat(diagnostics.ready()).isFalse();
        assertThat(diagnostics.diagnosticStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDiagnostics.STATUS_UNAVAILABLE);
        assertThat(diagnostics.issues()).containsExactly(AUDIT_RETENTION_BELOW_CONTRACT);
        assertThat(validation)
                .containsEntry("valid", false)
                .containsEntry("errorCount", 1);
        assertThat(diagnostics.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_FIX_AUDIT_CONFIG);
        assertThat(diagnostics.blockingRecommendationCount()).isEqualTo(1);
        assertThat(map(recommendations.get(0)))
                .containsEntry(
                        "action",
                        AgenticCommerceWayangPersistenceTransferAuditRecommendation.ACTION_FIX_AUDIT_CONFIG)
                .containsEntry("blocking", true);
        Map<String, Object> recommendationAttributes = map(map(recommendations.get(0)).get("attributes"));
        Map<String, Object> remediation = map(list(recommendationAttributes.get("remediation")).get(0));
        assertThat(recommendationAttributes)
                .containsEntry("remediationCount", 1)
                .containsEntry("patchCount", 1);
        assertThat(remediation)
                .containsEntry("code", AUDIT_RETENTION_BELOW_CONTRACT)
                .containsEntry("operation", "increase_retained_history")
                .containsEntry("suggestedValue", 2)
                .containsEntry("currentValue", 1);
        assertThat(map(list(recommendationAttributes.get("patches")).get(0)))
                .containsEntry("operation", AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE)
                .containsEntry("path", "$.maxTrails")
                .containsEntry("value", 2);
        Map<String, Object> patchApplication = map(recommendationAttributes.get("patchApplication"));
        assertThat(patchApplication)
                .containsEntry("patchable", true)
                .containsEntry("patchCount", 1)
                .containsEntry("resolved", true)
                .containsEntry("improved", true)
                .containsEntry("beforeValid", false)
                .containsEntry("afterValid", true);
        assertThat(strings(patchApplication.get("beforeErrorCodes")))
                .containsExactly(AUDIT_RETENTION_BELOW_CONTRACT);
        assertThat(strings(patchApplication.get("afterErrorCodes"))).isEmpty();
    }

    @Test
    void invalidAuditByteConfigRecommendationIncludesRemediationHint() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "file",
                        "journalPath",
                        "diagnostics/audit.jsonl",
                        "retentionPolicy",
                        Map.of(
                                "maxTrails",
                                10,
                                "maxBytes",
                                "64xb")));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(config);
        List<Object> recommendations = list(diagnostics.toMap().get("recommendations"));
        Map<String, Object> recommendationAttributes = map(map(recommendations.get(0)).get("attributes"));
        Map<String, Object> remediation = map(list(recommendationAttributes.get("remediation")).get(0));

        assertThat(diagnostics.issues()).containsExactly(AUDIT_RETENTION_BYTE_LIMIT_INVALID);
        assertThat(remediation)
                .containsEntry("code", AUDIT_RETENTION_BYTE_LIMIT_INVALID)
                .containsEntry("operation", "replace_with_byte_size")
                .containsEntry("expectedType", "byte-size")
                .containsEntry("rawValue", "64xb")
                .containsEntry("patchCount", 0);
        assertThat(recommendationAttributes)
                .containsEntry("patchCount", 0);
        Map<String, Object> patchApplication = map(recommendationAttributes.get("patchApplication"));
        assertThat(patchApplication)
                .containsEntry("patchable", false)
                .containsEntry("patchCount", 0)
                .containsEntry("resolved", false)
                .containsEntry("improved", false)
                .containsEntry("beforeValid", false)
                .containsEntry("afterValid", false);
        assertThat(strings(patchApplication.get("beforeErrorCodes")))
                .containsExactly(AUDIT_RETENTION_BYTE_LIMIT_INVALID);
        assertThat(strings(patchApplication.get("afterErrorCodes")))
                .containsExactly(AUDIT_RETENTION_BYTE_LIMIT_INVALID);
        assertThat(strings(remediation.get("examples")))
                .contains("64 KiB", "1MiB", "unlimited");
    }

    @Test
    void invalidAuditCountConfigRecommendationIncludesRemediationHint() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "file",
                        "journalPath",
                        "diagnostics/audit.jsonl",
                        "retentionPolicy",
                        Map.of(
                                "maxEvents",
                                "many")));

        AgenticCommerceWayangPersistenceTransferAuditDiagnostics diagnostics =
                AgenticCommerceWayangPersistenceTransferAuditDiagnostics.from(config);
        List<Object> recommendations = list(diagnostics.toMap().get("recommendations"));
        Map<String, Object> recommendationAttributes = map(map(recommendations.get(0)).get("attributes"));
        Map<String, Object> remediation = map(list(recommendationAttributes.get("remediation")).get(0));

        assertThat(diagnostics.issues()).containsExactly(AUDIT_RETENTION_COUNT_INVALID);
        assertThat(remediation)
                .containsEntry("code", AUDIT_RETENTION_COUNT_INVALID)
                .containsEntry("operation", "replace_with_integer")
                .containsEntry("expectedType", "integer")
                .containsEntry("minimumValue", 2)
                .containsEntry("rawValue", "many")
                .containsEntry("patchCount", 1);
        assertThat(map(list(recommendationAttributes.get("patches")).get(0)))
                .containsEntry("operation", AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE)
                .containsEntry("path", "$.retentionPolicy.maxTrails")
                .containsEntry("value", AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS);
        Map<String, Object> patchApplication = map(recommendationAttributes.get("patchApplication"));
        assertThat(patchApplication)
                .containsEntry("patchable", true)
                .containsEntry("patchCount", 1)
                .containsEntry("resolved", true)
                .containsEntry("afterValid", true);
        assertThat(strings(patchApplication.get("beforeErrorCodes")))
                .containsExactly(AUDIT_RETENTION_COUNT_INVALID);
        assertThat(strings(patchApplication.get("afterErrorCodes"))).isEmpty();
        assertThat(strings(remediation.get("acceptedKeys")))
                .contains("maxTrails", "maxEvents", "retention", "limit", "capacity");
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

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }
}
