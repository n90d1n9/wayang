package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_DUPLICATE_TARGETS;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_FIRST_CHILD_NOT_DURABLE;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_COMPOSITE_WITHOUT_DURABLE_CHILD;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_DATABASE_PROVIDER_GENERIC;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_JOURNAL_NOT_JSONL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_OBJECT_STORE_ENDPOINT_MISSING;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_BYTE_LIMIT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_RETENTION_COUNT_INVALID;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_DISABLED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.AUDIT_STORAGE_EPHEMERAL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditConfigValidationIssues.UNSUPPORTED_AUDIT_STORAGE_KIND;

class AgenticCommerceWayangPersistenceTransferAuditConfigValidationReportTest {

    @Test
    void fileAuditConfigIsValidWithoutFindings() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.file("agentic-commerce/audit.jsonl", 2);

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.errorCodes()).isEmpty();
        assertThat(report.warningCodes()).isEmpty();
        assertThat(map(config.toMap().get("validation")))
                .containsEntry("valid", true)
                .containsEntry("issueCount", 0);
        assertThat(map(report.toMap().get("target")))
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_FILE)
                .containsEntry("durable", true);
        assertThat(map(map(report.toMap().get("target")).get("retentionPolicy")))
                .containsEntry("maxTrails", 2)
                .containsEntry("maxBytes", 0L)
                .containsEntry("maxBytesDisplay", "unlimited")
                .containsEntry("byteLimited", false);
        assertThat(map(map(report.toMap().get("target")).get("retentionAssessment")))
                .containsEntry("expectedRetainedTrailCount", 2)
                .containsEntry("retainedTrailCount", 2)
                .containsEntry("satisfiesContract", true);
    }

    @Test
    void memoryAndNoopConfigsReportAdvisoryWarnings() {
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport memory =
                AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2).validationReport();
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport noop =
                AgenticCommerceWayangPersistenceTransferAuditConfig.noop().validationReport();

        assertThat(memory.valid()).isTrue();
        assertThat(memory.warningCodes()).containsExactly(AUDIT_STORAGE_EPHEMERAL);
        assertThat(noop.valid()).isTrue();
        assertThat(noop.warningCodes()).containsExactly(AUDIT_STORAGE_DISABLED);
    }

    @Test
    void retentionBelowContractReportsValidationError() {
        Map<String, Object> input = Map.of(
                "storageKind",
                "file",
                "journalPath",
                "agentic-commerce/audit.jsonl",
                "maxTrails",
                1);
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(input);

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();
        AgenticCommerceWayangPersistenceTransferAuditConfigPatch patch =
                report.remediations().get(0).patches().get(0);
        Map<String, Object> patched = report.applyPatchesTo(input);
        AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport application =
                report.patchApplicationReport(input);
        AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport configApplication =
                AgenticCommerceWayangPersistenceTransferAuditConfigPatchApplicationReport.fromConfig(config);

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCodes()).containsExactly(AUDIT_RETENTION_BELOW_CONTRACT);
        assertThat(report.warningCodes()).isEmpty();
        assertThat(report.patchCount()).isEqualTo(1);
        assertThat(report.patches()).containsExactly(patch);
        assertThat(map(report.issues().get(0).attributes()))
                .containsEntry("maxTrails", 1)
                .containsEntry("minimumRetainedTrailCount", 2);
        assertThat(report.toMap())
                .containsEntry("patchCount", 1);
        assertThat(map(list(report.toMap().get("patches")).get(0)))
                .containsEntry("path", "$.maxTrails")
                .containsEntry("value", 2);
        assertThat(report.remediationCount()).isEqualTo(1);
        assertThat(map(list(report.toMap().get("remediation")).get(0)))
                .containsEntry("code", AUDIT_RETENTION_BELOW_CONTRACT)
                .containsEntry(
                        "operation",
                        AgenticCommerceWayangPersistenceTransferAuditConfigRemediation
                                .OPERATION_INCREASE_RETAINED_HISTORY)
                .containsEntry("suggestedValue", 2)
                .containsEntry("patchCount", 1);
        assertThat(map(list(map(list(report.toMap().get("remediation")).get(0)).get("patches")).get(0)))
                .containsEntry("operation", AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE)
                .containsEntry("path", "$.maxTrails")
                .containsEntry("value", 2);
        assertThat(patched).containsEntry("maxTrails", 2);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(patched)
                .validationReport()
                .valid()).isTrue();
        assertThat(application.patchable()).isTrue();
        assertThat(application.patchCount()).isEqualTo(1);
        assertThat(application.beforeValid()).isFalse();
        assertThat(application.afterValid()).isTrue();
        assertThat(application.resolved()).isTrue();
        assertThat(application.improved()).isTrue();
        assertThat(application.patchedConfig()).containsEntry("maxTrails", 2);
        assertThat(application.toMap())
                .containsEntry("patchable", true)
                .containsEntry("resolved", true)
                .containsEntry("afterValid", true);
        assertThat(map(application.toMap().get("before"))).containsEntry("valid", false);
        assertThat(map(application.toMap().get("after"))).containsEntry("valid", true);
        assertThat(configApplication.resolved()).isTrue();
        assertThat(map(configApplication.patchedConfig().get("retentionPolicy")))
                .containsEntry("maxTrails", 2);
    }

    @Test
    void invalidRetentionCountReportsValidationError() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "file",
                        "journalPath",
                        "agentic-commerce/audit.jsonl",
                        "retentionPolicy",
                        Map.of(
                                "maxEvents",
                                "many")));

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();
        AgenticCommerceWayangPersistenceTransferAuditConfigPatch patch =
                report.remediations().get(0).patches().get(0);
        Map<String, Object> patched = report.applyPatchesTo(Map.of(
                "storageKind",
                "file",
                "journalPath",
                "agentic-commerce/audit.jsonl",
                "retentionPolicy",
                Map.of(
                        "maxEvents",
                        "many")));

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCodes()).containsExactly(AUDIT_RETENTION_COUNT_INVALID);
        assertThat(report.warningCodes()).isEmpty();
        assertThat(report.patchCount()).isEqualTo(1);
        assertThat(report.patches()).containsExactly(patch);
        assertThat(map(report.issues().get(0).attributes()))
                .containsEntry("rawValue", "many")
                .containsEntry("issue", AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy
                        .ISSUE_INVALID_MAX_TRAILS)
                .containsEntry("maxTrails", AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS);
        assertThat(map(map(map(report.toMap().get("target")).get("retentionPolicy")).get("attributes")))
                .containsKey("maxTrailsParse");
        assertThat(report.remediationCount()).isEqualTo(1);
        assertThat(map(list(report.toMap().get("remediation")).get(0)))
                .containsEntry("code", AUDIT_RETENTION_COUNT_INVALID)
                .containsEntry(
                        "operation",
                        AgenticCommerceWayangPersistenceTransferAuditConfigRemediation
                                .OPERATION_REPLACE_WITH_INTEGER)
                .containsEntry("rawValue", "many")
                .containsEntry("patchCount", 1);
        assertThat(map(list(map(list(report.toMap().get("remediation")).get(0)).get("patches")).get(0)))
                .containsEntry("operation", AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE)
                .containsEntry("path", "$.retentionPolicy.maxTrails")
                .containsEntry("value", AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS);
        assertThat(map(patched.get("retentionPolicy")))
                .containsEntry("maxTrails", AgenticCommerceWayangPersistenceTransferAuditConfig.DEFAULT_MAX_TRAILS);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(patched)
                .validationReport()
                .valid()).isTrue();
    }

    @Test
    void byteRetentionBelowContractReportsValidationError() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "file",
                        "journalPath",
                        "agentic-commerce/audit.jsonl",
                        "retentionPolicy",
                        Map.of(
                                "maxTrails",
                                10,
                                "maxBytes",
                                1)));

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();
        AgenticCommerceWayangPersistenceTransferAuditConfigPatch patch =
                report.remediations().get(0).patches().get(0);
        Map<String, Object> patched = report.applyPatchesTo(Map.of(
                "storageKind",
                "file",
                "journalPath",
                "agentic-commerce/audit.jsonl",
                "retentionPolicy",
                Map.of(
                        "maxTrails",
                        10,
                        "maxBytes",
                        1)));

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCodes()).containsExactly(AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT);
        assertThat(report.warningCodes()).isEmpty();
        assertThat(report.patchCount()).isEqualTo(1);
        assertThat(report.patches()).containsExactly(patch);
        assertThat(map(report.issues().get(0).attributes()))
                .containsEntry("maxBytes", 1L)
                .containsEntry("maxBytesDisplay", "1 B")
                .containsEntry("expectedRetainedTrailCount", 2)
                .containsEntry("retainedTrailCount", 1)
                .containsEntry("satisfiesContract", false);
        assertThat(map(report.issues().get(0).attributes()))
                .containsKey("recommendedMinBytesDisplay");
        assertThat((Long) map(report.issues().get(0).attributes()).get("recommendedMinBytes"))
                .isGreaterThan(1L);
        assertThat(map(map(report.toMap().get("target")).get("retentionAssessment")))
                .containsEntry("maxBytes", 1L)
                .containsEntry("satisfiesContract", false);
        assertThat(report.remediationCount()).isEqualTo(1);
        assertThat(map(list(report.toMap().get("remediation")).get(0)))
                .containsEntry("code", AUDIT_RETENTION_BYTE_LIMIT_BELOW_CONTRACT)
                .containsEntry(
                        "operation",
                        AgenticCommerceWayangPersistenceTransferAuditConfigRemediation
                                .OPERATION_INCREASE_BYTE_LIMIT)
                .containsEntry("currentValue", 1L)
                .containsEntry("patchCount", 1);
        assertThat(map(list(map(list(report.toMap().get("remediation")).get(0)).get("patches")).get(0)))
                .containsEntry("operation", AgenticCommerceWayangPersistenceTransferAuditConfigPatch.OPERATION_REPLACE)
                .containsEntry("path", "$.retentionPolicy.maxBytes")
                .containsKey("value");
        assertThat((Long) map(patched.get("retentionPolicy")).get("maxBytes")).isGreaterThan(1L);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(patched)
                .validationReport()
                .valid()).isTrue();
    }

    @Test
    void invalidByteRetentionReportsValidationError() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "file",
                        "journalPath",
                        "agentic-commerce/audit.jsonl",
                        "retentionPolicy",
                        Map.of(
                                "maxTrails",
                                10,
                                "maxBytes",
                                "64xb")));

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCodes()).containsExactly(AUDIT_RETENTION_BYTE_LIMIT_INVALID);
        assertThat(report.warningCodes()).isEmpty();
        assertThat(report.patchCount()).isZero();
        assertThat(report.patches()).isEmpty();
        assertThat(map(report.issues().get(0).attributes()))
                .containsEntry("rawValue", "64xb")
                .containsEntry("issue", AgenticCommerceWayangByteSizes.ISSUE_INVALID_BYTE_SIZE)
                .containsEntry("bytes", 0L)
                .containsEntry("bytesDisplay", "unlimited");
        assertThat(map(map(map(report.toMap().get("target")).get("retentionPolicy")).get("attributes")))
                .containsKey("maxBytesParse");
        assertThat(report.remediationCount()).isEqualTo(1);
        assertThat(map(list(report.toMap().get("remediation")).get(0)))
                .containsEntry("code", AUDIT_RETENTION_BYTE_LIMIT_INVALID)
                .containsEntry(
                        "operation",
                        AgenticCommerceWayangPersistenceTransferAuditConfigRemediation
                                .OPERATION_REPLACE_WITH_BYTE_SIZE)
                .containsEntry("rawValue", "64xb")
                .containsEntry("patchCount", 0);
        assertThat(report.patchApplicationReport(Map.of(
                "storageKind",
                "file",
                "journalPath",
                "agentic-commerce/audit.jsonl",
                "retentionPolicy",
                Map.of(
                        "maxTrails",
                        10,
                        "maxBytes",
                        "64xb"))).toMap())
                .containsEntry("patchable", false)
                .containsEntry("resolved", false)
                .containsEntry("afterValid", false);
    }

    @Test
    void unsupportedAuditStorageKindReportsProviderErrorUnlessCustomProviderExists() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "hosted-audit"));
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport defaultReport =
                config.validationReport();
        AgenticCommerceWayangPersistenceTransferAuditStoreProviders customProviders =
                AgenticCommerceWayangPersistenceTransferAuditStoreProviders.builder()
                        .providers(AgenticCommerceWayangPersistenceTransferAuditStoreProviders.defaults())
                        .provider(new HostedAuditProvider())
                        .build();

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport customReport =
                config.validationReport(customProviders);

        assertThat(defaultReport.valid()).isFalse();
        assertThat(defaultReport.errorCodes()).containsExactly(UNSUPPORTED_AUDIT_STORAGE_KIND);
        assertThat(customReport.valid()).isTrue();
        assertThat(customReport.errorCodes()).isEmpty();
    }

    @Test
    void rustFsAndGenericDatabaseConfigsReportProviderWarnings() {
        AgenticCommerceWayangPersistenceTransferAuditConfig rustFs =
                AgenticCommerceWayangPersistenceTransferAuditConfig.fromMap(Map.of(
                        "storageKind",
                        "rustfs",
                        "bucket",
                        "wayang-audit",
                        "journalObject",
                        "audit.log",
                        "maxTrails",
                        2));
        AgenticCommerceWayangPersistenceTransferAuditConfig database =
                AgenticCommerceWayangPersistenceTransferAuditConfig.database(
                        AgenticCommerceDatabasePersistenceConfig.defaults(),
                        "audit.log",
                        2);

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport rustFsReport =
                rustFs.validationReport();
        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport databaseReport =
                database.validationReport();

        assertThat(rustFsReport.valid()).isTrue();
        assertThat(rustFsReport.warningCodes())
                .containsExactly(
                        AUDIT_OBJECT_STORE_ENDPOINT_MISSING,
                        AUDIT_JOURNAL_NOT_JSONL);
        assertThat(databaseReport.valid()).isTrue();
        assertThat(databaseReport.warningCodes())
                .containsExactly(
                        AUDIT_DATABASE_PROVIDER_GENERIC,
                        AUDIT_JOURNAL_NOT_JSONL);
    }

    @Test
    void compositeAuditConfigReportsChildRisks() {
        AgenticCommerceWayangPersistenceTransferAuditConfig config =
                AgenticCommerceWayangPersistenceTransferAuditConfig.composite(List.of(
                        AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2),
                        AgenticCommerceWayangPersistenceTransferAuditConfig.memory(2)));

        AgenticCommerceWayangPersistenceTransferAuditConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes())
                .contains(
                        AUDIT_STORAGE_EPHEMERAL,
                        AUDIT_COMPOSITE_WITHOUT_DURABLE_CHILD,
                        AUDIT_COMPOSITE_FIRST_CHILD_NOT_DURABLE,
                        AUDIT_COMPOSITE_DUPLICATE_TARGETS);
        assertThat(map(report.toMap().get("target")))
                .containsEntry("storageKind", AgenticCommerceWayangPersistenceTransferAuditConfig.STORAGE_COMPOSITE)
                .containsEntry("durable", false);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
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
