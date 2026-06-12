package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceTransferTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void copiesAllDocumentsFromFileStoreToObjectStore() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        InMemoryAgenticCommerceObjectStoreClient objectClient = InMemoryAgenticCommerceObjectStoreClient.create();
        ObjectStoreAgenticCommerceWayangPersistenceStore target =
                ObjectStoreAgenticCommerceWayangPersistenceStore.configured(
                        AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                                "provider",
                                "s3",
                                "bucket",
                                "wayang-transfer",
                                "keyPrefix",
                                "backup")),
                        objectClient);

        AgenticCommerceWayangPersistenceTransferReport report =
                AgenticCommerceWayangPersistenceTransfer.copyAll().copy(source, target);

        assertThat(report.passed()).isTrue();
        assertThat(report.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_COMPLETE);
        assertThat(report.summary().complete()).isTrue();
        assertThat(report.summary().mutatedTarget()).isTrue();
        assertThat(report.summary().wouldMutateTarget()).isFalse();
        assertThat(report.summary().targetChanged()).isTrue();
        assertThat(report.summary().attentionReasons()).isEmpty();
        assertThat(report.findings()).isEmpty();
        assertThat(report.findingCount()).isZero();
        assertThat(report.findingIndex().findings()).isEmpty();
        assertThat(report.findingIndex().codes()).isEmpty();
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = report.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_COMPLETE);
        assertThat(auditEvent.successful()).isTrue();
        assertThat(auditEvent.mutatedTarget()).isTrue();
        assertThat(auditEvent.blocked()).isFalse();
        assertThat(auditEvent.copiedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = report.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_COMPLETE);
        assertThat(auditTrail.successful()).isTrue();
        assertThat(auditTrail.eventCount()).isEqualTo(1);
        assertThat(auditTrail.eventTypes())
                .containsExactly(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
        assertThat(auditTrail.latestEvent().eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_COMPLETE);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.hasType(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY))
                .isTrue();
        assertThat(eventIndex.type(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY))
                .hasSize(1);
        assertThat(eventIndex.successful()).hasSize(1);
        assertThat(eventIndex.blocked()).isEmpty();
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE);
        assertThat(auditSummary.requiresAttention()).isFalse();
        assertThat(auditSummary.operatorActionRecommended()).isFalse();
        assertThat(auditSummary.mutatedTarget()).isTrue();
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_COMPLETE);
        assertThat(auditDecision.terminal()).isTrue();
        assertThat(auditDecision.inspectionRecommended()).isFalse();
        assertThat(report.copiedDocumentCount()).isEqualTo(4);
        assertThat(report.copiedDocuments())
                .containsExactlyElementsOf(AgenticCommerceWayangPersistenceDocuments.ids());
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.action()).isEqualTo("copied");
                    assertThat(status.copied()).isTrue();
                    assertThat(status.copyable()).isTrue();
                });
        assertThat(report.skippedDocumentCount()).isZero();
        assertThat(report.toMap().toString())
                .doesNotContain(AgenticCommerceWayangPersistenceContractHarness.CONTRACT_BEARER_TOKEN);
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo(AgenticCommerceWayangPersistenceContractHarness.CONTRACT_BEARER_TOKEN);
        assertThat(target.loadBootstrapConfig()).isPresent();
        assertThat(target.loadBootstrapReport()).isPresent();
        assertThat(target.loadManifest()).isPresent();
        assertThat(objectClient.contains("wayang-transfer", "backup/runtime-config.json")).isTrue();
        assertThat(map(report.toMap().get("attributes")))
                .containsEntry("transferId", AgenticCommerceWayangPersistenceTransfer.TRANSFER_ID)
                .containsEntry("verified", true);
        assertThat(report.sourcePersistenceTarget())
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(report.targetPersistenceTargetBefore())
                .containsEntry("targetKind", "object-store")
                .containsEntry("provider", AgenticCommerceObjectStoreConfig.PROVIDER_S3)
                .containsEntry("location", "wayang-transfer/backup")
                .containsEntry("cloudStorage", true);
        assertThat(report.targetPersistenceTargetAfter())
                .containsEntry("targetKind", "object-store")
                .containsEntry("location", "wayang-transfer/backup");
        assertThat(report.persistenceTargetComparisonBefore().changeReasons())
                .contains(
                        "storage_kind_changed",
                        "target_kind_changed",
                        "provider_changed",
                        "location_changed",
                        "cloud_storage_changed");
        assertThat(report.persistenceTargetComparisonAfter().changed()).isTrue();
        assertThat(map(report.toMap().get("sourcePersistenceTarget")))
                .containsEntry("targetKind", "file");
        assertThat(map(report.toMap().get("targetPersistenceTargetAfter")))
                .containsEntry("targetKind", "object-store")
                .containsEntry("location", "wayang-transfer/backup");
        assertThat(map(report.toMap().get("persistenceTargetComparisonAfter")))
                .containsEntry("changed", true)
                .containsEntry("cloudStorageChanged", true);
        assertThat(report.toMap()).containsEntry("transferStatus", "complete");
        assertThat(map(report.toMap().get("summary")))
                .containsEntry("transferStatus", "complete")
                .containsEntry("mutatedTarget", true)
                .containsEntry("targetChanged", true)
                .containsEntry("copiedDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(report.toMap().get("auditEvent")))
                .containsEntry(
                        "eventType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY)
                .containsEntry("eventStatus", "complete")
                .containsEntry("successful", true)
                .containsEntry("mutatedTarget", true)
                .containsEntry("copiedDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(report.toMap().get("auditTrail")))
                .containsEntry(
                        "trailType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY)
                .containsEntry("trailStatus", "complete")
                .containsEntry("successful", true)
                .containsEntry("eventCount", 1);
        Map<String, Object> copyAuditTrail = map(report.toMap().get("auditTrail"));
        assertThat(list(copyAuditTrail.get("events"))).hasSize(1);
        assertThat(map(copyAuditTrail.get("eventIndex")))
                .containsEntry("eventCount", 1)
                .containsEntry("successfulEventCount", 1)
                .containsEntry("blockedEventCount", 0);
        assertThat(map(map(copyAuditTrail.get("eventIndex")).get("eventsByType")))
                .containsKey(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
        assertThat(map(copyAuditTrail.get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE)
                .containsEntry("requiresAttention", false)
                .containsEntry("operatorActionRecommended", false);
        assertThat(map(map(copyAuditTrail.get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP)
                .containsEntry("decisionStatus", AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_COMPLETE)
                .containsEntry("terminal", true);
        assertThat(report.toMap())
                .containsEntry("findingCount", 0)
                .containsEntry("errorFindingCount", 0)
                .containsEntry("warningFindingCount", 0)
                .containsEntry("infoFindingCount", 0);
        assertThat(map(report.toMap().get("findingIndex")))
                .containsEntry("findingCount", 0)
                .containsEntry("blockingFindingCount", 0)
                .containsEntry("documentFindingCount", 0);
        assertThat(list(report.toMap().get("documents")))
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count());
    }

    @Test
    void dryRunPlansDocumentsWithoutWritingTarget() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("dry-run-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("dry-run-target"));

        AgenticCommerceWayangPersistenceTransferReport report =
                AgenticCommerceWayangPersistenceTransfer.configured(
                                AgenticCommerceWayangPersistenceTransferOptions.dryRunOnly())
                        .copy(source, target);

        assertThat(report.passed()).isTrue();
        assertThat(report.options().dryRun()).isTrue();
        assertThat(report.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_PREVIEW);
        assertThat(report.summary().dryRun()).isTrue();
        assertThat(report.summary().complete()).isTrue();
        assertThat(report.summary().mutatedTarget()).isFalse();
        assertThat(report.summary().wouldMutateTarget()).isTrue();
        assertThat(report.summary().attentionReasons()).containsExactly("dry_run");
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = report.auditTrail().summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_PREVIEW);
        assertThat(auditSummary.dryRun()).isTrue();
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.operatorActionRecommended()).isFalse();
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_INSPECT);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_PREVIEW_ONLY);
        assertThat(auditDecision.inspectionRecommended()).isTrue();
        assertThat(auditDecision.operatorApprovalRequired()).isFalse();
        assertThat(report.findingCount()).isEqualTo(1);
        assertThat(report.errorFindingCount()).isZero();
        assertThat(report.warningFindingCount()).isZero();
        assertThat(report.infoFindingCount()).isEqualTo(1);
        assertThat(report.findings().get(0).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO)
                .containsEntry("source", AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER)
                .containsEntry("code", AgenticCommerceWayangPersistenceTransferFindings.DRY_RUN_PREVIEW)
                .containsEntry("blocking", false);
        assertThat(report.findingIndex().hasCode(AgenticCommerceWayangPersistenceTransferFindings.DRY_RUN_PREVIEW))
                .isTrue();
        assertThat(report.findingIndex().infos()).hasSize(1);
        assertThat(report.findingIndex().source(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER))
                .hasSize(1);
        assertThat(report.plannedDocumentCount()).isEqualTo(4);
        assertThat(report.copiedDocumentCount()).isZero();
        assertThat(report.copiedDocuments()).isEmpty();
        assertThat(report.skippedDocumentCount()).isZero();
        assertThat(report.blockedDocumentCount()).isZero();
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.action()).isEqualTo("would_copy");
                    assertThat(status.dryRun()).isTrue();
                    assertThat(status.planningOnly()).isFalse();
                });
        assertThat(list(report.toMap().get("documents")))
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(document -> assertThat(map(document))
                        .containsEntry("action", "would_copy")
                        .containsEntry("dryRun", true)
                        .containsEntry("planningOnly", false));
        assertThat(target.loadRuntimeConfig()).isEmpty();
        assertThat(target.loadBootstrapConfig()).isEmpty();
        assertThat(target.loadBootstrapReport()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
        assertThat(map(report.toMap().get("options"))).containsEntry("dryRun", true);
        assertThat(map(report.toMap().get("summary")))
                .containsEntry("transferStatus", "preview")
                .containsEntry("dryRun", true)
                .containsEntry("wouldMutateTarget", true)
                .containsEntry("copiedDocumentCount", 0);
        assertThat(map(map(report.toMap().get("auditTrail")).get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_PREVIEW)
                .containsEntry("dryRun", true)
                .containsEntry("requiresAttention", true);
        assertThat(map(map(map(report.toMap().get("auditTrail")).get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_INSPECT)
                .containsEntry(
                        "decisionStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_PREVIEW_ONLY);
        assertThat(map(list(report.toMap().get("findings")).get(0)))
                .containsEntry("code", AgenticCommerceWayangPersistenceTransferFindings.DRY_RUN_PREVIEW)
                .containsEntry("severity", AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_INFO);
        Map<String, Object> findingIndex = map(report.toMap().get("findingIndex"));
        assertThat(findingIndex)
                .containsEntry("findingCount", 1)
                .containsEntry("infoFindingCount", 1);
        assertThat(map(findingIndex.get("findingsByCode")))
                .containsKey(AgenticCommerceWayangPersistenceTransferFindings.DRY_RUN_PREVIEW);
        assertThat(map(report.toMap().get("attributes")))
                .containsEntry("dryRun", true)
                .containsEntry("verified", false);
    }

    @Test
    void planPreviewsDocumentsWithoutWritingTarget() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("plan-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("plan-target"));

        AgenticCommerceWayangPersistenceTransferPlan plan =
                AgenticCommerceWayangPersistenceTransfer.copyAll().plan(source, target);

        assertThat(plan.passed()).isTrue();
        assertThat(plan.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_PREVIEW);
        assertThat(plan.summary().planningOnly()).isTrue();
        assertThat(plan.summary().complete()).isTrue();
        assertThat(plan.summary().wouldMutateTarget()).isTrue();
        assertThat(plan.summary().targetChanged()).isTrue();
        assertThat(plan.summary().attentionReasons()).containsExactly("planning_only");
        assertThat(plan.findingCount()).isEqualTo(1);
        assertThat(plan.infoFindingCount()).isEqualTo(1);
        assertThat(plan.findings().get(0).toMap())
                .containsEntry("source", AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER)
                .containsEntry("code", AgenticCommerceWayangPersistenceTransferFindings.PLANNING_ONLY_PREVIEW);
        assertThat(plan.findingIndex().code(AgenticCommerceWayangPersistenceTransferFindings.PLANNING_ONLY_PREVIEW))
                .hasSize(1);
        assertThat(plan.findingIndex().sources())
                .containsExactly(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER);
        assertThat(plan.plannedDocumentCount()).isEqualTo(4);
        assertThat(plan.copyableDocumentCount()).isEqualTo(4);
        assertThat(plan.blockedDocumentCount()).isZero();
        assertThat(plan.wouldMutateTarget()).isTrue();
        assertThat(plan.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.action()).isEqualTo("would_copy");
                    assertThat(status.planningOnly()).isTrue();
                    assertThat(status.copyable()).isTrue();
                });
        assertThat(list(plan.toMap().get("documents")))
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(document -> assertThat(map(document))
                        .containsEntry("action", "would_copy")
                        .containsEntry("planningOnly", true)
                        .containsEntry("copyable", true));
        assertThat(target.loadRuntimeConfig()).isEmpty();
        assertThat(target.loadBootstrapConfig()).isEmpty();
        assertThat(target.loadBootstrapReport()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
        assertThat(plan.toMap()).containsEntry("planningOnly", true);
        assertThat(map(plan.toMap().get("options"))).containsEntry("dryRun", false);
        assertThat(plan.sourcePersistenceTarget()).containsEntry("targetKind", "file");
        assertThat(plan.targetPersistenceTarget()).containsEntry("targetKind", "file");
        assertThat(plan.persistenceTargetComparison().locationChanged()).isTrue();
        assertThat(map(plan.toMap().get("sourcePersistenceTarget"))).containsEntry("targetKind", "file");
        assertThat(map(plan.toMap().get("targetPersistenceTarget"))).containsEntry("targetKind", "file");
        assertThat(map(plan.toMap().get("persistenceTargetComparison")))
                .containsEntry("changed", true)
                .containsEntry("locationChanged", true);
        assertThat(plan.toMap()).containsEntry("transferStatus", "preview");
        assertThat(map(plan.toMap().get("summary")))
                .containsEntry("transferStatus", "preview")
                .containsEntry("planningOnly", true)
                .containsEntry("wouldMutateTarget", true)
                .containsEntry("copyableDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(list(plan.toMap().get("findings")).get(0)))
                .containsEntry("code", AgenticCommerceWayangPersistenceTransferFindings.PLANNING_ONLY_PREVIEW);
        assertThat(map(plan.toMap().get("findingIndex")))
                .containsEntry("findingCount", 1)
                .containsEntry("infoFindingCount", 1);
        assertThat(map(plan.toMap().get("attributes")))
                .containsEntry("planningOnly", true)
                .containsEntry("dryRun", false);
    }

    @Test
    void noOverwritePreservesExistingTargetDocuments() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("no-overwrite-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("no-overwrite-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("target-token")
                        .withBaseUrl("https://target.example"))
                .build());

        AgenticCommerceWayangPersistenceTransferReport report =
                AgenticCommerceWayangPersistenceTransfer.configured(
                                AgenticCommerceWayangPersistenceTransferOptions.noOverwrite())
                        .copy(source, target);

        assertThat(report.passed()).isTrue();
        assertThat(report.options().overwriteExisting()).isFalse();
        assertThat(report.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_PARTIAL);
        assertThat(report.summary().partial()).isTrue();
        assertThat(report.summary().blocked()).isTrue();
        assertThat(report.summary().copiedDocumentCount()).isEqualTo(3);
        assertThat(report.summary().copyableDocumentCount()).isEqualTo(3);
        assertThat(report.summary().attentionReasons()).containsExactly("blocked_documents");
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = report.auditTrail().summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_ATTENTION);
        assertThat(auditSummary.successful()).isTrue();
        assertThat(auditSummary.blocked()).isTrue();
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.copiedDocumentCount()).isEqualTo(3);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_INSPECT);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_ATTENTION_REQUIRED);
        assertThat(auditDecision.operatorApprovalRequired()).isTrue();
        assertThat(auditDecision.reasons()).contains("blocked_documents");
        assertThat(report.findingCount()).isEqualTo(1);
        assertThat(report.warningFindingCount()).isEqualTo(1);
        assertThat(report.findings().get(0).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceTransferFinding.SEVERITY_WARNING)
                .containsEntry("source", AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT)
                .containsEntry("code", AgenticCommerceWayangPersistenceTransferFindings.DOCUMENT_BLOCKED_EXISTING)
                .containsEntry("documentId", AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(report.findingIndex().document(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG))
                .hasSize(1);
        assertThat(report.findingIndex().warnings()).hasSize(1);
        assertThat(report.findingIndex().documentScoped()).hasSize(1);
        assertThat(report.plannedDocumentCount()).isEqualTo(4);
        assertThat(report.copiedDocumentCount()).isEqualTo(3);
        assertThat(report.blockedDocuments())
                .containsExactly(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        AgenticCommerceWayangPersistenceTransferDocumentStatus runtimeStatus =
                status(report.documents(), AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(runtimeStatus.action()).isEqualTo("blocked");
        assertThat(runtimeStatus.copyable()).isFalse();
        assertThat(runtimeStatus.blocked()).isTrue();
        AgenticCommerceWayangPersistenceTransferDocumentStatus bootstrapStatus =
                status(report.documents(), AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_CONFIG);
        assertThat(bootstrapStatus.action()).isEqualTo("copied");
        assertThat(bootstrapStatus.copied()).isTrue();
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("target-token");
        assertThat(target.loadBootstrapConfig()).isPresent();
        assertThat(target.loadBootstrapReport()).isPresent();
        assertThat(target.loadManifest()).isPresent();
        assertThat(map(report.toMap().get("attributes")))
                .containsEntry("overwriteExisting", false)
                .containsEntry("verified", true);
    }

    @Test
    void noOverwritePlanIdentifiesBlockedDocumentsWithoutWritingTarget() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("no-overwrite-plan-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("no-overwrite-plan-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("target-plan-token"))
                .build());

        AgenticCommerceWayangPersistenceTransferPlan plan =
                AgenticCommerceWayangPersistenceTransfer.configured(
                                AgenticCommerceWayangPersistenceTransferOptions.noOverwrite())
                        .plan(source, target);

        assertThat(plan.passed()).isTrue();
        assertThat(plan.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_PREVIEW);
        assertThat(plan.summary().partial()).isTrue();
        assertThat(plan.summary().blocked()).isTrue();
        assertThat(plan.summary().planningOnly()).isTrue();
        assertThat(plan.summary().copyableDocumentCount()).isEqualTo(3);
        assertThat(plan.summary().attentionReasons())
                .containsExactly("blocked_documents", "planning_only");
        assertThat(findingCodes(plan.findings()))
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferFindings.DOCUMENT_BLOCKED_EXISTING,
                        AgenticCommerceWayangPersistenceTransferFindings.PLANNING_ONLY_PREVIEW);
        assertThat(plan.warningFindingCount()).isEqualTo(1);
        assertThat(plan.infoFindingCount()).isEqualTo(1);
        assertThat(plan.findingIndex().document(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG))
                .hasSize(1);
        assertThat(plan.findingIndex().source(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT))
                .hasSize(1);
        assertThat(plan.findingIndex().source(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TRANSFER))
                .hasSize(1);
        assertThat(plan.plannedDocumentCount()).isEqualTo(4);
        assertThat(plan.copyableDocumentCount()).isEqualTo(3);
        assertThat(plan.blockedDocuments())
                .containsExactly(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(plan.wouldMutateTarget()).isTrue();
        AgenticCommerceWayangPersistenceTransferDocumentStatus runtimeStatus =
                status(plan.documents(), AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(runtimeStatus.action()).isEqualTo("blocked");
        assertThat(runtimeStatus.copyable()).isFalse();
        assertThat(runtimeStatus.planningOnly()).isTrue();
        assertThat(status(plan.documents(), AgenticCommerceWayangPersistenceTransfer.DOCUMENT_MANIFEST)
                .action()).isEqualTo("would_copy");
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("target-plan-token");
        assertThat(target.loadBootstrapConfig()).isEmpty();
        assertThat(target.loadBootstrapReport()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
        assertThat(map(plan.toMap().get("options"))).containsEntry("overwriteExisting", false);
    }

    @Test
    void skipsMissingSourceDocumentsWithoutFailing() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("empty-source"));
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("target"));

        AgenticCommerceWayangPersistenceTransferReport report =
                AgenticCommerceWayangPersistenceTransfer.copyAll().copy(source, target);

        assertThat(report.passed()).isTrue();
        assertThat(report.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_SKIPPED);
        assertThat(report.summary().noop()).isTrue();
        assertThat(report.summary().skippedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.summary().attentionReasons()).containsExactly("skipped_documents");
        assertThat(report.findingCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.infoFindingCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(findingCodes(report.findings()))
                .containsOnly(AgenticCommerceWayangPersistenceTransferFindings.DOCUMENT_SKIPPED_MISSING_SOURCE);
        assertThat(report.findingIndex().source(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_DOCUMENT))
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.findingIndex().document(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_MANIFEST))
                .hasSize(1);
        assertThat(report.copiedDocumentCount()).isZero();
        assertThat(report.skippedDocuments())
                .containsExactlyInAnyOrder(
                        AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG,
                        AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_CONFIG,
                        AgenticCommerceWayangPersistenceTransfer.DOCUMENT_BOOTSTRAP_REPORT,
                        AgenticCommerceWayangPersistenceTransfer.DOCUMENT_MANIFEST);
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.action()).isEqualTo("skipped");
                    assertThat(status.planned()).isFalse();
                    assertThat(status.copyable()).isFalse();
                });
        assertThat(target.loadRuntimeConfig()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
    }

    @Test
    void serviceTransfersCurrentStoreToTarget() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-target"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferReport report = service.transferTo(target);

        assertThat(report.passed()).isTrue();
        assertThat(target.loadRuntimeConfig()).isPresent();
        assertThat(target.loadBootstrapConfig()).isPresent();
        assertThat(target.loadBootstrapReport()).isPresent();
        assertThat(target.loadManifest()).isPresent();
    }

    @Test
    void serviceTransfersWithOptions() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-option-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-option-target"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferReport report =
                service.transferTo(target, AgenticCommerceWayangPersistenceTransferOptions.dryRunOnly());

        assertThat(report.passed()).isTrue();
        assertThat(report.options().dryRun()).isTrue();
        assertThat(report.plannedDocumentCount()).isEqualTo(4);
        assertThat(target.loadRuntimeConfig()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
    }

    @Test
    void servicePlansTransferWithOptions() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-plan-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("service-plan-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("service-target-token"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferPlan plan =
                service.planTransferTo(target, AgenticCommerceWayangPersistenceTransferOptions.noOverwrite());

        assertThat(plan.passed()).isTrue();
        assertThat(plan.copyableDocumentCount()).isEqualTo(3);
        assertThat(plan.blockedDocuments())
                .containsExactly(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("service-target-token");
        assertThat(target.loadManifest()).isEmpty();
    }

    @Test
    void servicePreflightsTransferReadinessWithoutWritingTarget() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("preflight-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("preflight-target"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferPreflightReport preflight =
                service.preflightTransferTo(target);

        assertThat(preflight.preflightStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY);
        assertThat(preflight.passed()).isTrue();
        assertThat(preflight.readyToApply()).isTrue();
        assertThat(preflight.sourceReady()).isTrue();
        assertThat(preflight.targetReady()).isTrue();
        assertThat(preflight.blocked()).isFalse();
        assertThat(preflight.wouldMutateTarget()).isTrue();
        assertThat(preflight.attentionReasons()).containsExactly("target_health_warnings");
        assertThat(preflight.checkCount()).isEqualTo(3);
        assertThat(preflight.readyCheckCount()).isEqualTo(3);
        assertThat(preflight.blockingCheckCount()).isZero();
        assertThat(preflight.checks())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightCheck::checkId)
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TARGET_HEALTH,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN);
        assertThat(preflight.checks())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightCheck::checkStatus)
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_PASSED,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_WARNING,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_PASSED);
        assertThat(preflight.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = preflight.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY);
        assertThat(auditEvent.successful()).isTrue();
        assertThat(auditEvent.blocked()).isFalse();
        assertThat(auditEvent.copyableDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(auditEvent.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = preflight.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY);
        assertThat(auditTrail.eventCount()).isEqualTo(1);
        assertThat(auditTrail.eventStatuses())
                .containsExactly(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY);
        assertThat(auditTrail.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.hasType(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT))
                .isTrue();
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY))
                .hasSize(1);
        assertThat(eventIndex.successful()).hasSize(1);
        assertThat(eventIndex.blocked()).isEmpty();
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_READY);
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.operatorActionRecommended()).isTrue();
        assertThat(auditSummary.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_APPLY);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_READY_TO_APPLY);
        assertThat(auditDecision.terminal()).isFalse();
        assertThat(auditDecision.targetMutationExpected()).isTrue();
        assertThat(auditDecision.operatorApprovalRequired()).isFalse();
        assertThat(preflight.recommendationCount()).isEqualTo(2);
        assertThat(preflight.blockingRecommendationCount()).isZero();
        assertThat(preflight.recommendations().get(0).toMap())
                .containsEntry(
                        "action",
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER)
                .containsEntry(
                        "priority",
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.PRIORITY_PRIMARY)
                .containsEntry("blocking", false);
        assertThat(preflight.sourceHealth().complete()).isTrue();
        assertThat(preflight.targetHealth().complete()).isFalse();
        assertThat(preflight.plan().wouldMutateTarget()).isTrue();
        assertThat(target.loadRuntimeConfig()).isEmpty();
        assertThat(target.loadManifest()).isEmpty();
        assertThat(preflight.toMap())
                .containsEntry("preflightStatus", "ready")
                .containsEntry("readyToApply", true)
                .containsEntry("wouldMutateTarget", true);
        assertThat(preflight.toMap())
                .containsEntry("checkCount", 3)
                .containsEntry("readyCheckCount", 3)
                .containsEntry("blockingCheckCount", 0);
        assertThat(preflight.toMap())
                .containsEntry("recommendationCount", 2)
                .containsEntry("blockingRecommendationCount", 0);
        Map<String, Object> checksById = map(preflight.toMap().get("checksById"));
        assertThat(checksById)
                .containsKeys(
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TARGET_HEALTH,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN);
        assertThat(map(checksById.get(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TARGET_HEALTH)))
                .containsEntry("checkStatus", AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_WARNING)
                .containsEntry("ready", true)
                .containsEntry("blocking", false);
        assertThat(map(list(preflight.toMap().get("recommendations")).get(0)))
                .containsEntry(
                        "action",
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_APPLY_TRANSFER);
        assertThat(map(preflight.toMap().get("sourceHealthSummary")))
                .containsEntry("complete", true);
        assertThat(map(preflight.toMap().get("targetHealthSummary")))
                .containsEntry("complete", false);
        assertThat(map(preflight.toMap().get("planSummary")))
                .containsEntry("transferStatus", "preview");
        assertThat(map(preflight.toMap().get("auditEvent")))
                .containsEntry(
                        "eventType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT)
                .containsEntry("eventStatus", "ready")
                .containsEntry("successful", true)
                .containsEntry("copyableDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(preflight.toMap().get("auditTrail")))
                .containsEntry(
                        "trailType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT)
                .containsEntry("trailStatus", "ready")
                .containsEntry("successful", true)
                .containsEntry("eventCount", 1);
        assertThat(map(map(preflight.toMap().get("auditTrail")).get("eventIndex")))
                .containsEntry("eventCount", 1)
                .containsEntry("successfulEventCount", 1)
                .containsEntry("blockedEventCount", 0);
        assertThat(map(map(preflight.toMap().get("auditTrail")).get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_READY)
                .containsEntry("requiresAttention", true)
                .containsEntry("operatorActionRecommended", true);
        assertThat(map(map(map(preflight.toMap().get("auditTrail")).get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_APPLY)
                .containsEntry(
                        "decisionStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_READY_TO_APPLY)
                .containsEntry("targetMutationExpected", true);
        assertThat(map(preflight.toMap().get("attributes")))
                .containsEntry("preflightId", "agentic-commerce-wayang-persistence-transfer-preflight")
                .containsEntry("sourceStoreKind", "file")
                .containsEntry("targetStoreKind", "file");
    }

    @Test
    void servicePreflightIdentifiesBlockedNoOverwriteTransfer() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("blocked-preflight-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("blocked-preflight-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("blocked-target-token"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferPreflightReport preflight =
                service.preflightTransferTo(target, AgenticCommerceWayangPersistenceTransferOptions.noOverwrite());

        assertThat(preflight.preflightStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED);
        assertThat(preflight.passed()).isTrue();
        assertThat(preflight.readyToApply()).isFalse();
        assertThat(preflight.blocked()).isTrue();
        assertThat(preflight.wouldMutateTarget()).isTrue();
        assertThat(preflight.plan().blockedDocuments())
                .containsExactly(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(preflight.attentionReasons())
                .contains("target_health_warnings", "transfer_blocked_documents");
        assertThat(preflight.readyCheckCount()).isEqualTo(2);
        assertThat(preflight.blockingCheckCount()).isEqualTo(1);
        assertThat(preflight.blockingChecks())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightCheck::checkId)
                .containsExactly(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN);
        assertThat(preflight.blockingChecks().get(0).checkStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_BLOCKED);
        assertThat(preflight.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        assertThat(preflight.blockingRecommendationCount()).isEqualTo(1);
        assertThat(preflight.blockingRecommendations())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightRecommendation::action)
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET);
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = preflight.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED);
        assertThat(auditEvent.successful()).isFalse();
        assertThat(auditEvent.blocked()).isTrue();
        assertThat(auditEvent.blockedDocumentCount()).isEqualTo(1);
        assertThat(auditEvent.recommendationActions())
                .contains(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET);
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = preflight.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED);
        assertThat(auditTrail.successful()).isFalse();
        assertThat(auditTrail.blocked()).isTrue();
        assertThat(auditTrail.eventCount()).isEqualTo(1);
        assertThat(auditTrail.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED))
                .hasSize(1);
        assertThat(eventIndex.successful()).isEmpty();
        assertThat(eventIndex.blocked()).hasSize(1);
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED);
        assertThat(auditSummary.successful()).isFalse();
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.operatorActionRecommended()).isTrue();
        assertThat(auditSummary.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCE_OR_CLEAR_REQUIRED);
        assertThat(auditDecision.forceRequired()).isTrue();
        assertThat(auditDecision.targetMutationExpected()).isTrue();
        assertThat(auditDecision.operatorApprovalRequired()).isTrue();
        assertThat(map(preflight.blockingRecommendations().get(0).attributes()))
                .containsEntry("blockedDocumentCount", 1);
        assertThat(map(preflight.toMap().get("auditEvent")))
                .containsEntry(
                        "eventType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT)
                .containsEntry("eventStatus", "blocked")
                .containsEntry("successful", false)
                .containsEntry("blocked", true);
        assertThat(map(preflight.toMap().get("auditTrail")))
                .containsEntry("trailStatus", "blocked")
                .containsEntry("successful", false)
                .containsEntry("blocked", true)
                .containsEntry("eventCount", 1);
        assertThat(map(map(preflight.toMap().get("auditTrail")).get("eventIndex")))
                .containsEntry("eventCount", 1)
                .containsEntry("successfulEventCount", 0)
                .containsEntry("blockedEventCount", 1);
        assertThat(map(map(preflight.toMap().get("auditTrail")).get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED)
                .containsEntry("successful", false)
                .containsEntry("operatorActionRecommended", true);
        assertThat(map(map(map(preflight.toMap().get("auditTrail")).get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE)
                .containsEntry(
                        "decisionStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCE_OR_CLEAR_REQUIRED)
                .containsEntry("forceRequired", true);
        assertThat(map(preflight.toMap().get("planFindingIndex")))
                .containsEntry("warningFindingCount", 1)
                .containsEntry("infoFindingCount", 1);
        assertThat(map(preflight.toMap().get("checksById")))
                .containsKey(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_TRANSFER_PLAN);
        assertThat(preflight.toMap())
                .containsEntry("recommendationCount", 2)
                .containsEntry("blockingRecommendationCount", 1);
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("blocked-target-token");
        assertThat(target.loadManifest()).isEmpty();
    }

    @Test
    void servicePreflightIdentifiesIncompleteSource() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("incomplete-source"));
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("incomplete-target"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferPreflightReport preflight =
                service.preflightTransferTo(target);

        assertThat(preflight.preflightStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_SOURCE_INCOMPLETE);
        assertThat(preflight.passed()).isTrue();
        assertThat(preflight.readyToApply()).isFalse();
        assertThat(preflight.sourceReady()).isFalse();
        assertThat(preflight.targetReady()).isTrue();
        assertThat(preflight.wouldMutateTarget()).isFalse();
        assertThat(preflight.attentionReasons())
                .contains(
                        "source_documents_missing",
                        "source_health_warnings",
                        "target_health_warnings",
                        "target_would_not_change");
        assertThat(preflight.readyCheckCount()).isEqualTo(1);
        assertThat(preflight.blockingCheckCount()).isEqualTo(1);
        assertThat(preflight.blockingChecks())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightCheck::checkId)
                .containsExactly(AgenticCommerceWayangPersistenceTransferPreflightCheck.CHECK_SOURCE_HEALTH);
        assertThat(preflight.checks())
                .extracting(AgenticCommerceWayangPersistenceTransferPreflightCheck::checkStatus)
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_INCOMPLETE,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_WARNING,
                        AgenticCommerceWayangPersistenceTransferPreflightCheck.STATUS_NOOP);
        assertThat(preflight.recommendationActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_COMPLETE_SOURCE_DOCUMENTS,
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation.ACTION_REVIEW_TARGET_WARNINGS);
        assertThat(preflight.blockingRecommendationCount()).isEqualTo(1);
        assertThat(preflight.blockingRecommendations().get(0).action())
                .isEqualTo(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_COMPLETE_SOURCE_DOCUMENTS);
        assertThat(map(preflight.blockingRecommendations().get(0).attributes()).get("missingDocumentIds"))
                .isInstanceOf(List.class);
        assertThat(preflight.plan().skippedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(list(preflight.toMap().get("sourceMissingDocumentIds")))
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(preflight.toMap().get("planSummary")))
                .containsEntry("transferStatus", "skipped")
                .containsEntry("wouldMutateTarget", false);
    }

    @Test
    void serviceApplyTransferRunsWhenPreflightIsReady() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-target"));
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferApplyReport applyReport =
                service.applyTransferTo(target);

        assertThat(applyReport.applyStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_APPLIED);
        assertThat(applyReport.passed()).isTrue();
        assertThat(applyReport.applied()).isTrue();
        assertThat(applyReport.forced()).isFalse();
        assertThat(applyReport.transferAttempted()).isTrue();
        assertThat(applyReport.blockedByPreflight()).isFalse();
        assertThat(applyReport.preflight().readyToApply()).isTrue();
        assertThat(applyReport.transferReport()).isNotNull();
        assertThat(applyReport.transferReport().copiedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = applyReport.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_APPLIED);
        assertThat(auditEvent.successful()).isTrue();
        assertThat(auditEvent.forced()).isFalse();
        assertThat(auditEvent.mutatedTarget()).isTrue();
        assertThat(auditEvent.blocked()).isFalse();
        assertThat(auditEvent.copiedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = applyReport.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_APPLIED);
        assertThat(auditTrail.successful()).isTrue();
        assertThat(auditTrail.mutatedTarget()).isTrue();
        assertThat(auditTrail.eventCount()).isEqualTo(3);
        assertThat(auditTrail.eventTypes())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditTrail.eventStatuses())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_READY,
                        AgenticCommerceWayangPersistenceTransferSummary.STATUS_COMPLETE,
                        AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_APPLIED);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.type(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY))
                .hasSize(1);
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_APPLIED))
                .hasSize(1);
        assertThat(eventIndex.successful()).hasSize(3);
        assertThat(eventIndex.blocked()).isEmpty();
        assertThat(eventIndex.mutatedTarget()).hasSize(2);
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE);
        assertThat(auditSummary.requiresAttention()).isFalse();
        assertThat(auditSummary.operatorActionRecommended()).isFalse();
        assertThat(auditSummary.mutatedTarget()).isTrue();
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_COMPLETE);
        assertThat(auditDecision.terminal()).isTrue();
        assertThat(auditDecision.targetMutationExpected()).isFalse();
        assertThat(target.loadRuntimeConfig()).isPresent();
        assertThat(target.loadManifest()).isPresent();
        assertThat(applyReport.toMap())
                .containsEntry("applyStatus", "applied")
                .containsEntry("transferAttempted", true)
                .containsEntry("transferReportAvailable", true)
                .containsEntry("mutatedTarget", true);
        assertThat(map(applyReport.toMap().get("auditEvent")))
                .containsEntry(
                        "eventType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY)
                .containsEntry("eventStatus", "applied")
                .containsEntry("successful", true)
                .containsEntry("mutatedTarget", true);
        Map<String, Object> applyAuditTrail = map(applyReport.toMap().get("auditTrail"));
        assertThat(applyAuditTrail)
                .containsEntry("trailStatus", "applied")
                .containsEntry("successful", true)
                .containsEntry("eventCount", 3);
        assertThat(list(applyAuditTrail.get("eventTypes")))
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(map(applyAuditTrail.get("latestEvent"))).containsEntry("eventStatus", "applied");
        assertThat(map(applyAuditTrail.get("eventIndex")))
                .containsEntry("eventCount", 3)
                .containsEntry("successfulEventCount", 3)
                .containsEntry("blockedEventCount", 0)
                .containsEntry("mutatedTargetEventCount", 2);
        assertThat(map(applyAuditTrail.get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE)
                .containsEntry("requiresAttention", false)
                .containsEntry("operatorActionRecommended", false);
        assertThat(map(map(applyAuditTrail.get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP)
                .containsEntry("decisionStatus", AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_COMPLETE)
                .containsEntry("terminal", true);
        assertThat(map(map(applyAuditTrail.get("eventIndex")).get("eventsByType")))
                .containsKeys(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(map(applyReport.toMap().get("attributes")))
                .containsEntry("applyId", "agentic-commerce-wayang-persistence-transfer-apply")
                .containsEntry("forced", false);
    }

    @Test
    void serviceApplyTransferBlocksWhenPreflightIsNotReady() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-block-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-block-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("guard-target-token"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferApplyReport applyReport =
                service.applyTransferTo(target, AgenticCommerceWayangPersistenceTransferOptions.noOverwrite());

        assertThat(applyReport.applyStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_BLOCKED);
        assertThat(applyReport.passed()).isFalse();
        assertThat(applyReport.applied()).isFalse();
        assertThat(applyReport.transferAttempted()).isFalse();
        assertThat(applyReport.transferReportAvailable()).isFalse();
        assertThat(applyReport.blockedByPreflight()).isTrue();
        assertThat(applyReport.preflight().preflightStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED);
        assertThat(applyReport.preflight().recommendationActions())
                .contains(
                        AgenticCommerceWayangPersistenceTransferPreflightRecommendation
                                .ACTION_ENABLE_OVERWRITE_OR_CLEAR_TARGET);
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = applyReport.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_BLOCKED);
        assertThat(auditEvent.successful()).isFalse();
        assertThat(auditEvent.blocked()).isTrue();
        assertThat(auditEvent.mutatedTarget()).isFalse();
        assertThat(auditEvent.copiedDocumentCount()).isZero();
        assertThat(auditEvent.blockedDocumentCount()).isEqualTo(1);
        assertThat(map(auditEvent.attributes()))
                .containsEntry("transferAttempted", false)
                .containsEntry("blockedByPreflight", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = applyReport.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_BLOCKED);
        assertThat(auditTrail.successful()).isFalse();
        assertThat(auditTrail.blocked()).isTrue();
        assertThat(auditTrail.mutatedTarget()).isFalse();
        assertThat(auditTrail.eventCount()).isEqualTo(2);
        assertThat(auditTrail.hasEventType(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY))
                .isFalse();
        assertThat(auditTrail.eventTypes())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.hasType(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY))
                .isFalse();
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_BLOCKED))
                .hasSize(2);
        assertThat(eventIndex.successful()).isEmpty();
        assertThat(eventIndex.blocked()).hasSize(2);
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED);
        assertThat(auditSummary.successful()).isFalse();
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.operatorActionRecommended()).isTrue();
        assertThat(auditSummary.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCE_OR_CLEAR_REQUIRED);
        assertThat(auditDecision.forceRequired()).isTrue();
        assertThat(auditDecision.retryRecommended()).isFalse();
        assertThat(auditDecision.operatorApprovalRequired()).isTrue();
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("guard-target-token");
        assertThat(target.loadManifest()).isEmpty();
        assertThat(applyReport.toMap())
                .containsEntry("applyStatus", "blocked")
                .containsEntry("transferAttempted", false)
                .containsEntry("transferReportAvailable", false)
                .containsEntry("blockedByPreflight", true);
        assertThat(map(applyReport.toMap().get("auditEvent")))
                .containsEntry("eventStatus", "blocked")
                .containsEntry("successful", false)
                .containsEntry("blocked", true);
        Map<String, Object> blockedAuditTrail = map(applyReport.toMap().get("auditTrail"));
        assertThat(blockedAuditTrail)
                .containsEntry("trailStatus", "blocked")
                .containsEntry("successful", false)
                .containsEntry("blocked", true)
                .containsEntry("eventCount", 2);
        assertThat(map(blockedAuditTrail.get("attributes")))
                .containsEntry("transferAttempted", false)
                .containsEntry("blockedByPreflight", true);
        assertThat(map(blockedAuditTrail.get("eventIndex")))
                .containsEntry("eventCount", 2)
                .containsEntry("successfulEventCount", 0)
                .containsEntry("blockedEventCount", 2);
        assertThat(map(blockedAuditTrail.get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_BLOCKED)
                .containsEntry("requiresAttention", true)
                .containsEntry("operatorActionRecommended", true);
        assertThat(map(map(blockedAuditTrail.get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE)
                .containsEntry(
                        "decisionStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCE_OR_CLEAR_REQUIRED)
                .containsEntry("operatorApprovalRequired", true);
        assertThat(applyReport.toMap()).doesNotContainKey("transferReport");
    }

    @Test
    void serviceApplyTransferCanForcePastPreflightGuard() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-force-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        FileAgenticCommerceWayangPersistenceStore target =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("apply-force-target"));
        target.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("force-target-token"))
                .build());
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);

        AgenticCommerceWayangPersistenceTransferApplyReport applyReport =
                service.applyTransferTo(
                        target,
                        AgenticCommerceWayangPersistenceTransferOptions.noOverwrite(),
                        true);

        assertThat(applyReport.applyStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_FORCED);
        assertThat(applyReport.passed()).isTrue();
        assertThat(applyReport.forced()).isTrue();
        assertThat(applyReport.transferAttempted()).isTrue();
        assertThat(applyReport.blockedByPreflight()).isFalse();
        assertThat(applyReport.preflight().readyToApply()).isFalse();
        assertThat(applyReport.transferReport().copiedDocumentCount()).isEqualTo(3);
        assertThat(applyReport.transferReport().blockedDocuments())
                .containsExactly(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        AgenticCommerceWayangPersistenceTransferAuditEvent auditEvent = applyReport.auditEvent();
        assertThat(auditEvent.eventType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditEvent.eventStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_FORCED);
        assertThat(auditEvent.successful()).isTrue();
        assertThat(auditEvent.forced()).isTrue();
        assertThat(auditEvent.mutatedTarget()).isTrue();
        assertThat(auditEvent.copiedDocumentCount()).isEqualTo(3);
        assertThat(auditEvent.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditTrail auditTrail = applyReport.auditTrail();
        assertThat(auditTrail.trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(auditTrail.trailStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_FORCED);
        assertThat(auditTrail.successful()).isTrue();
        assertThat(auditTrail.forced()).isTrue();
        assertThat(auditTrail.blocked()).isTrue();
        assertThat(auditTrail.eventCount()).isEqualTo(3);
        assertThat(auditTrail.eventStatuses())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED,
                        AgenticCommerceWayangPersistenceTransferSummary.STATUS_PARTIAL,
                        AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_FORCED);
        assertThat(auditTrail.copiedDocumentCount()).isEqualTo(3);
        assertThat(auditTrail.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditTrailIndex eventIndex = auditTrail.eventIndex();
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferPreflightReport.STATUS_BLOCKED))
                .hasSize(1);
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferSummary.STATUS_PARTIAL))
                .hasSize(1);
        assertThat(eventIndex.status(AgenticCommerceWayangPersistenceTransferApplyReport.STATUS_FORCED))
                .hasSize(1);
        assertThat(eventIndex.successful()).hasSize(2);
        assertThat(eventIndex.blocked()).hasSize(3);
        assertThat(eventIndex.forced()).hasSize(1);
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = auditTrail.summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FORCED);
        assertThat(auditSummary.successful()).isTrue();
        assertThat(auditSummary.requiresAttention()).isTrue();
        assertThat(auditSummary.operatorActionRecommended()).isTrue();
        assertThat(auditSummary.forced()).isTrue();
        assertThat(auditSummary.blockedDocumentCount()).isEqualTo(1);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCED_COMPLETE);
        assertThat(auditDecision.terminal()).isTrue();
        assertThat(auditDecision.inspectionRecommended()).isTrue();
        assertThat(auditDecision.operatorApprovalRequired()).isTrue();
        assertThat(target.loadRuntimeConfig().orElseThrow().connectorConfig().bearerToken())
                .isEqualTo("force-target-token");
        assertThat(target.loadManifest()).isPresent();
        assertThat(map(applyReport.toMap().get("transferReport")))
                .containsEntry("transferStatus", "partial")
                .containsEntry("copiedDocumentCount", 3);
        assertThat(map(applyReport.toMap().get("auditEvent")))
                .containsEntry("eventStatus", "forced")
                .containsEntry("successful", true)
                .containsEntry("forced", true)
                .containsEntry("copiedDocumentCount", 3);
        assertThat(map(applyReport.toMap().get("auditTrail")))
                .containsEntry("trailStatus", "forced")
                .containsEntry("successful", true)
                .containsEntry("forced", true)
                .containsEntry("eventCount", 3);
        assertThat(map(map(applyReport.toMap().get("auditTrail")).get("eventIndex")))
                .containsEntry("eventCount", 3)
                .containsEntry("successfulEventCount", 2)
                .containsEntry("blockedEventCount", 3)
                .containsEntry("forcedEventCount", 1);
        assertThat(map(map(applyReport.toMap().get("auditTrail")).get("summary")))
                .containsEntry(
                        "outcomeStatus",
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FORCED)
                .containsEntry("requiresAttention", true)
                .containsEntry("operatorActionRecommended", true);
        assertThat(map(map(map(applyReport.toMap().get("auditTrail")).get("summary")).get("decision")))
                .containsEntry("nextAction", AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP)
                .containsEntry(
                        "decisionStatus",
                        AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCED_COMPLETE)
                .containsEntry("operatorApprovalRequired", true);
        assertThat(map(applyReport.toMap().get("attributes"))).containsEntry("forced", true);
    }

    @Test
    void targetFailuresAreReported() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("failing-source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);

        AgenticCommerceWayangPersistenceTransferReport report =
                AgenticCommerceWayangPersistenceTransfer.copyAll().copy(source, new FailingPersistenceStore());

        assertThat(report.passed()).isFalse();
        assertThat(report.summary().transferStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferSummary.STATUS_FAILED);
        assertThat(report.summary().failed()).isTrue();
        assertThat(report.summary().failedDocumentCount())
                .isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.summary().attentionReasons())
                .containsExactly("issues_present", "failed_documents");
        AgenticCommerceWayangPersistenceTransferAuditSummary auditSummary = report.auditTrail().summary();
        assertThat(auditSummary.outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FAILED);
        AgenticCommerceWayangPersistenceTransferAuditDecision auditDecision = auditSummary.decision();
        assertThat(auditDecision.nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_RETRY);
        assertThat(auditDecision.decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FAILED_RETRYABLE);
        assertThat(auditDecision.retryRecommended()).isTrue();
        assertThat(auditDecision.inspectionRecommended()).isTrue();
        assertThat(report.errorFindingCount()).isEqualTo(report.issueCount());
        assertThat(report.warningFindingCount()).isZero();
        assertThat(report.infoFindingCount()).isZero();
        assertThat(findingCodes(report.findings()))
                .contains(
                        "target_status_before_failed",
                        "target_status_after_failed",
                        "runtime_config_save_failed",
                        "bootstrap_config_save_failed",
                        "bootstrap_report_save_failed",
                        "manifest_save_failed");
        assertThat(report.findingIndex().errors()).hasSize(report.issueCount());
        assertThat(report.findingIndex().blocking()).hasSize(report.issueCount());
        assertThat(report.findingIndex().source(AgenticCommerceWayangPersistenceTransferFinding.SOURCE_TARGET))
                .hasSize(2);
        assertThat(report.findingIndex().document(AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG))
                .hasSize(1);
        assertThat(report.issues())
                .contains(
                        "target_status_before_failed",
                        "runtime_config_save_failed",
                        "bootstrap_config_save_failed",
                        "bootstrap_report_save_failed",
                        "manifest_save_failed");
        assertThat(report.copiedDocumentCount()).isZero();
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(document -> assertThat(document.action()).isEqualTo("failed"));
        AgenticCommerceWayangPersistenceTransferDocumentStatus runtimeStatus =
                status(report.documents(), AgenticCommerceWayangPersistenceTransfer.DOCUMENT_RUNTIME_CONFIG);
        assertThat(runtimeStatus.failed()).isTrue();
        assertThat(runtimeStatus.issues()).contains("runtime_config_save_failed");
        assertThat(map(list(report.toMap().get("documents")).get(0)))
                .containsEntry("action", "failed")
                .containsEntry("failed", true);
        assertThat(report.targetPersistenceTargetBefore())
                .containsEntry("storageKind", "failing")
                .containsEntry("targetKind", "failing");
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

    private static AgenticCommerceWayangPersistenceTransferDocumentStatus status(
            List<AgenticCommerceWayangPersistenceTransferDocumentStatus> documents,
            String id) {
        return documents.stream()
                .filter(document -> document.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> findingCodes(
            List<AgenticCommerceWayangPersistenceTransferFinding> findings) {
        return findings.stream()
                .map(AgenticCommerceWayangPersistenceTransferFinding::code)
                .toList();
    }

    private static final class FailingPersistenceStore implements AgenticCommerceWayangPersistenceStore {

        @Override
        public String storageKind() {
            return "failing";
        }

        @Override
        public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
            throw failure();
        }

        @Override
        public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
            throw failure();
        }

        @Override
        public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
            throw failure();
        }

        @Override
        public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadBootstrapReport() {
            throw failure();
        }

        @Override
        public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadManifest() {
            throw failure();
        }

        @Override
        public void saveManifest(AgenticCommerceWayangManifest manifest) {
            throw failure();
        }

        @Override
        public Map<String, Object> toMap() {
            throw failure();
        }

        private static IllegalStateException failure() {
            return new IllegalStateException("store unavailable");
        }
    }
}
