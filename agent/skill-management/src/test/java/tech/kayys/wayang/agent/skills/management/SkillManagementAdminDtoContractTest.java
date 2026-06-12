package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementAdminDtoContractTest {

    @Test
    void adminDtoRecordShapeMatchesApiContract() {
        assertThat(contract(
                SkillManagementAdminInspection.class,
                SkillManagementAdminStoreStatus.class,
                SkillManagementAdminReconcileStatus.class,
                SkillManagementAdminDeploymentReport.class,
                SkillManagementAdminMaintenanceReport.class,
                SkillManagementAdminMaintenanceStepReport.class,
                SkillManagementAdminMaintenanceStepHistorySummary.class,
                SkillManagementAdminDeploymentPreflightReport.class,
                SkillManagementAdminPersistenceStrategy.class,
                SkillManagementAdminPersistenceRole.class,
                SkillManagementAdminPersistenceProfileCatalog.class,
                SkillManagementAdminPersistenceProfile.class,
                SkillManagementAdminValidationReport.class,
                SkillManagementAdminDeploymentHistoryPage.class,
                SkillManagementAdminDeploymentHistoryEntry.class,
                SkillManagementAdminOperationTracePage.class,
                SkillManagementAdminOperationTrace.class,
                SkillManagementAdminEventPage.class,
                SkillManagementAdminEvent.class,
                SkillManagementAdminEventSummary.class))
                .isEqualTo("""
                        SkillManagementAdminInspection
                          ready:boolean
                          lifecycleStateConsistent:boolean
                          definitionStore:SkillManagementAdminStoreStatus
                          lifecycleStateStore:SkillManagementAdminStoreStatus
                          eventStore:SkillManagementAdminStoreStatus
                          artifactStore:SkillManagementAdminStoreStatus
                          lifecycleStateReconciliation:SkillManagementAdminReconcileStatus

                        SkillManagementAdminStoreStatus
                          name:String
                          storeType:String
                          status:String
                          ready:boolean
                          itemCount:int
                          itemIds:List<String>
                          statusCounts:Map<String, Integer>
                          failure:String
                          children:List<SkillManagementAdminStoreStatus>
                          capabilities:List<String>

                        SkillManagementAdminReconcileStatus
                          consistent:boolean
                          definitionSkillIds:List<String>
                          persistedStateSkillIds:List<String>
                          missingStateSkillIds:List<String>
                          orphanedStateSkillIds:List<String>
                          createdStateSkillIds:List<String>
                          removedStateSkillIds:List<String>
                          failure:String

                        SkillManagementAdminDeploymentReport
                          dryRun:boolean
                          changed:boolean
                          consistent:boolean
                          maintenance:SkillManagementAdminMaintenanceReport

                        SkillManagementAdminMaintenanceReport
                          dryRun:boolean
                          changed:boolean
                          consistent:boolean
                          definitionSync:SkillManagementAdminDefinitionSyncStatus
                          artifactSync:SkillManagementAdminArtifactSyncStatus
                          lifecycleStateReconciliation:SkillManagementAdminReconcileStatus
                          eventPrune:SkillManagementAdminEventPruneReport
                          steps:List<SkillManagementAdminMaintenanceStepReport>

                        SkillManagementAdminMaintenanceStepReport
                          step:String
                          status:String
                          dryRun:boolean
                          skipped:boolean
                          changed:boolean
                          consistent:boolean
                          changes:long
                          conflicts:long
                          failure:String

                        SkillManagementAdminMaintenanceStepHistorySummary
                          step:String
                          deployments:int
                          dryRunDeployments:int
                          skippedDeployments:int
                          changedDeployments:int
                          consistentDeployments:int
                          failedDeployments:int
                          changes:long
                          conflicts:long

                        SkillManagementAdminDeploymentPreflightReport
                          ready:boolean
                          deployable:boolean
                          errorCount:int
                          message:String
                          errors:List<String>
                          configuration:SkillManagementAdminValidationReport
                          targetStores:SkillManagementAdminValidationReport
                          sourceStores:SkillManagementAdminValidationReport
                          capabilities:SkillManagementAdminValidationReport

                        SkillManagementAdminPersistenceStrategy
                          strategy:String
                          fullyDurable:boolean
                          hasEphemeralRole:boolean
                          hasDurableFallback:boolean
                          hasExternalProvider:boolean
                          hasCustomProvider:boolean
                          hasCompositeProvider:boolean
                          hasMirroredProvider:boolean
                          roleCount:int
                          durableRoleCount:int
                          ephemeralRoleCount:int
                          disabledRoleCount:int
                          customRoleCount:int
                          warningCount:int
                          warnings:List<String>
                          roles:List<SkillManagementAdminPersistenceRole>

                        SkillManagementAdminPersistenceRole
                          role:String
                          path:String
                          provider:String
                          persistenceClass:String
                          strategy:String
                          disabled:boolean
                          ephemeral:boolean
                          durable:boolean
                          durableFallback:boolean
                          external:boolean
                          custom:boolean
                          composite:boolean
                          mirrored:boolean
                          capabilities:List<String>
                          children:List<SkillManagementAdminPersistenceRole>

                        SkillManagementAdminPersistenceProfileCatalog
                          profileCount:int
                          durableProfileCount:int
                          externalProfileCount:int
                          compositeProfileCount:int
                          mirroredProfileCount:int
                          durableFallbackProfileCount:int
                          profiles:List<SkillManagementAdminPersistenceProfile>

                        SkillManagementAdminPersistenceProfile
                          label:String
                          aliases:List<String>
                          description:String
                          persistence:SkillManagementAdminPersistenceStrategy

                        SkillManagementAdminValidationReport
                          valid:boolean
                          errorCount:int
                          message:String
                          errors:List<String>

                        SkillManagementAdminDeploymentHistoryPage
                          matchedDeployments:int
                          returnedDeployments:int
                          truncated:boolean
                          successfulDeployments:int
                          failedDeployments:int
                          changedDeployments:int
                          consistentDeployments:int
                          preflightDeployments:int
                          preflightConfigurationFailures:int
                          preflightTargetStoreFailures:int
                          preflightSourceStoreFailures:int
                          preflightCapabilityFailures:int
                          stepSummaries:List<SkillManagementAdminMaintenanceStepHistorySummary>
                          deployments:List<SkillManagementAdminDeploymentHistoryEntry>

                        SkillManagementAdminDeploymentHistoryEntry
                          occurredAt:String
                          operationId:String
                          parentOperationId:String
                          success:boolean
                          dryRun:boolean
                          changed:boolean
                          consistent:boolean
                          definitionChanges:int
                          artifactChanges:int
                          artifactConflicts:int
                          lifecycleCreated:int
                          lifecycleRemoved:int
                          eventPruneEnabled:boolean
                          eventPruneSkipped:boolean
                          eventPruneChanged:boolean
                          eventPruned:int
                          preflightAvailable:boolean
                          preflightReady:boolean
                          preflightDeployable:boolean
                          preflightErrors:int
                          preflightConfigurationErrors:int
                          preflightTargetStoreErrors:int
                          preflightSourceStoreErrors:int
                          preflightCapabilityErrors:int
                          preflightMessage:String
                          errorType:String
                          error:String
                          steps:List<SkillManagementAdminMaintenanceStepReport>

                        SkillManagementAdminOperationTracePage
                          matchedRootEvents:int
                          returnedRootEvents:int
                          traceableRootEvents:int
                          untraceableRootEvents:int
                          filteredTraces:int
                          returnedTraces:int
                          truncated:boolean
                          childEventLimit:int
                          healthyTraces:int
                          failedTraces:int
                          rootMissingTraces:int
                          missingOperationIdTraces:int
                          statusFilter:String
                          traces:List<SkillManagementAdminOperationTrace>

                        SkillManagementAdminOperationTrace
                          operationId:String
                          rootEventAvailable:boolean
                          totalEvents:int
                          successfulEvents:int
                          failedEvents:int
                          childEventCount:int
                          healthy:boolean
                          failed:boolean
                          failedChildEvents:int
                          status:String
                          summary:SkillManagementAdminEventSummary
                          rootEvent:SkillManagementAdminEvent
                          childEvents:List<SkillManagementAdminEvent>

                        SkillManagementAdminEventPage
                          matchedEvents:int
                          returnedEvents:int
                          truncated:boolean
                          summary:SkillManagementAdminEventSummary
                          events:List<SkillManagementAdminEvent>

                        SkillManagementAdminEvent
                          occurredAt:String
                          operation:String
                          skillId:String
                          operationId:String
                          parentOperationId:String
                          success:boolean
                          attributes:Map<String, String>

                        SkillManagementAdminEventSummary
                          totalEvents:int
                          successfulEvents:int
                          failedEvents:int
                          operationCounts:Map<String, Integer>
                          skillCounts:Map<String, Integer>
                        """.stripTrailing());
    }

    private static String contract(Class<?>... types) {
        return List.of(types).stream()
                .map(SkillManagementAdminDtoContractTest::contract)
                .collect(Collectors.joining("\n\n"));
    }

    private static String contract(Class<?> type) {
        assertThat(type.isRecord()).as("%s should remain a record DTO", type.getSimpleName()).isTrue();
        return type.getSimpleName()
                + "\n"
                + List.of(type.getRecordComponents()).stream()
                        .map(SkillManagementAdminDtoContractTest::component)
                        .collect(Collectors.joining("\n"));
    }

    private static String component(RecordComponent component) {
        return "  " + component.getName() + ":" + typeName(component);
    }

    private static String typeName(RecordComponent component) {
        return component.getGenericType()
                .getTypeName()
                .replace("tech.kayys.wayang.agent.skills.management.", "")
                .replace("java.util.", "")
                .replace("java.lang.", "");
    }
}
