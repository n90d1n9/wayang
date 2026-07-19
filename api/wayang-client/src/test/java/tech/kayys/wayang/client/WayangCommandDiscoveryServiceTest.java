package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.alignment.WayangStandardAlignmentContract;
import tech.kayys.wayang.catalog.WayangStandardCatalogContract;
import tech.kayys.wayang.command.WayangCommandDiscoveryContract;
import tech.kayys.wayang.command.WayangCommandDiscoveryService;
import tech.kayys.wayang.contract.WayangContractKey;
import tech.kayys.wayang.readiness.WayangReadinessContract;
import tech.kayys.wayang.skill.WayangSkillContract;
import tech.kayys.wayang.workbench.WayangWorkbenchCatalog;
import tech.kayys.wayang.workbench.WayangWorkbenchContract;
import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandContract;
import tech.kayys.wayang.workbench.WorkbenchCommandDiscovery;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangCommandDiscoveryServiceTest {

    private final WayangCommandDiscoveryService discovery = WayangCommandDiscoveryService.create();

    @Test
    void returnsAllCommandsForEmptyQuery() {
        assertThat(discovery.discover(WayangWorkbenchCatalog.localCommands(), WorkbenchCommandQuery.all()))
                .containsExactlyElementsOf(WayangWorkbenchCatalog.localCommands());
        assertThat(discovery.discover(WayangWorkbenchCatalog.localCommands(), null))
                .containsExactlyElementsOf(WayangWorkbenchCatalog.localCommands());
    }

    @Test
    void normalizesAndComposesSurfaceCategoryAndIdFilters() {
        WorkbenchCommandQuery query = new WorkbenchCommandQuery(
                " assistant-agent ",
                " runs ",
                " run-session-context ");

        assertThat(query.surfaceId()).isEqualTo("assistant-agent");
        assertThat(query.profileId()).isNull();
        assertThat(query.resolvedSurfaceId()).isEqualTo("assistant-agent");
        assertThat(query.category()).isEqualTo("runs");
        assertThat(query.commandId()).isEqualTo("run-session-context");
        assertThat(query.contractJsonSchemaId()).isNull();
        assertThat(query.filtered()).isTrue();
        assertThat(discovery.discover(WayangWorkbenchCatalog.localCommands(), query))
                .singleElement()
                .satisfies(command -> {
                    assertThat(command.id()).isEqualTo("run-session-context");
                    assertThat(command.surfaceIds()).containsExactly("assistant-agent");
                });
        assertThat(discovery.discover(
                        WayangWorkbenchCatalog.localCommands(),
                        WorkbenchCommandQuery.forCommandId(" run-session-context ")))
                .singleElement()
                .satisfies(command -> assertThat(command.id()).isEqualTo("run-session-context"));
        assertThat(discovery.discover(
                        WayangWorkbenchCatalog.localCommands(),
                        WorkbenchCommandQuery.forCategory(" Run Specs ")))
                .extracting(WorkbenchCommand::id)
                .contains("run-spec-dry-json", "run-print-spec-output");
        assertThat(discovery.discover(
                        WayangWorkbenchCatalog.localCommands(),
                        WorkbenchCommandQuery.forSurface(" assistant-agent ")))
                .extracting(WorkbenchCommand::id)
                .contains("run-assistant-surface", "run-session-context")
                .doesNotContain("workspace-inspect");
    }

    @Test
    void returnsDiscoveryMetadataForMatchingCommands() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.of("assistant-agent", "Runs", null));

        assertThat(result.query().surfaceId()).isEqualTo("assistant-agent");
        assertThat(result.query().category()).isEqualTo("Runs");
        assertThat(result.totalCommands()).isEqualTo(WayangWorkbenchCatalog.localCommands().size());
        assertThat(result.matchingCommands()).isEqualTo(result.commands().size());
        assertThat(result.categories()).containsExactly("Runs");
        assertThat(result.categoryCounts()).containsEntry("Runs", result.matchingCommands());
        assertThat(result.categorySummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("Runs");
                    assertThat(summary.count()).isEqualTo(result.matchingCommands());
                    assertThat(summary.commandIds()).contains("run-assistant-surface", "run-session-context");
                });
        assertThat(result.commandIds())
                .contains("run-assistant-surface", "run-session-context")
                .doesNotContain("workspace-inspect", "harness-plan");
        assertThat(result.empty()).isFalse();
    }

    @Test
    void filtersCommandsByContractJsonSchemaId() {
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);
        WorkbenchCommandQuery query = WorkbenchCommandQuery.forContractJsonSchemaId(
                " urn:wayang:contract:wayang.run.planning:v1:run-preview ");
        WorkbenchCommandQuery keyedQuery = WorkbenchCommandQuery.forContractKey(previewKey);

        assertThat(query.contractJsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(query.contractJsonSchemaKey()).hasValue(previewKey);
        assertThat(query.filtered()).isTrue();
        assertThat(keyedQuery.contractJsonSchemaId()).isEqualTo(previewKey.jsonSchemaId());
        assertThat(keyedQuery.contractJsonSchemaKey()).hasValue(previewKey);
        assertThat(discovery.commandDiscovery(WayangWorkbenchCatalog.localCommands(), keyedQuery).commandIds())
                .containsExactly("run-dry-json", "run-spec-dry-json");

        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                query);

        assertThat(result.query().contractJsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(result.matchingCommands()).isEqualTo(2);
        assertThat(result.categories()).containsExactly("Runs", "Run Specs");
        assertThat(result.categoryCounts())
                .containsEntry("Runs", 1)
                .containsEntry("Run Specs", 1);
        assertThat(result.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(result.contractJsonSchemaIdCounts())
                .containsExactly(java.util.Map.entry(
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview",
                        2));
        assertThat(result.contractKeys()).containsExactly(previewKey);
        assertThat(result.contractKeyCounts())
                .containsExactly(java.util.Map.entry(previewKey, 2));
        assertThat(result.commandIdsForContractKey(previewKey))
                .containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.key()).isEqualTo(previewKey);
                    assertThat(summary.jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                    assertThat(summary.schema()).isEqualTo("wayang.run.planning");
                    assertThat(summary.version()).isEqualTo(1);
                    assertThat(summary.envelope()).isEqualTo("run-preview");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
        assertThat(result.contractSummariesForKey(previewKey))
                .singleElement()
                .satisfies(summary -> assertThat(summary.commandIds())
                        .containsExactly("run-dry-json", "run-spec-dry-json"));
        assertThat(result.contractSummaryByJsonSchemaId(
                        " urn:wayang:contract:wayang.run.planning:v1:run-preview "))
                .hasValueSatisfying(summary -> assertThat(summary.key()).isEqualTo(previewKey));
        assertThat(result.commands())
                .allSatisfy(command -> assertThat(command.contracts())
                        .anySatisfy(contract -> assertThat(contract.jsonSchemaId())
                                .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview")));
    }

    @Test
    void preservesExactMatchingForCustomContractJsonSchemaIds() {
        WayangContractKey customKey = WayangContractKey.of("custom.contract", 1, "custom-envelope");
        WorkbenchCommand custom = WorkbenchCommand.shared(
                "custom-contract-json",
                "Custom Contract JSON",
                "custom --json",
                "Custom",
                "Custom schema id command.",
                java.util.List.of(),
                java.util.List.of(WorkbenchCommandContract.of(
                        "custom.contract",
                        1,
                        "custom-envelope",
                        "custom-schema-id")));

        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                java.util.List.of(custom),
                WorkbenchCommandQuery.forContractJsonSchemaId(" custom-schema-id "));

        assertThat(result.matchingCommands()).isEqualTo(1);
        assertThat(result.commandIds()).containsExactly("custom-contract-json");
        assertThat(result.contractJsonSchemaIds()).containsExactly("custom-schema-id");
        assertThat(result.contractKeys()).containsExactly(customKey);
        assertThat(result.contractKeyCounts()).containsExactly(java.util.Map.entry(customKey, 1));
        assertThat(result.commandIdsForContractKey(customKey)).containsExactly("custom-contract-json");
        assertThat(result.contractSummaryByJsonSchemaId(" custom-schema-id "))
                .hasValueSatisfying(summary -> {
                    assertThat(summary.key()).isEqualTo(customKey);
                    assertThat(summary.jsonSchemaId()).isEqualTo("custom-schema-id");
                });
        assertThat(result.query().contractJsonSchemaKey()).isEmpty();
    }

    @Test
    void filtersCommandDiscoveryCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.command.discovery:v1:commands-discovery"));

        assertThat(result.matchingCommands()).isEqualTo(5);
        assertThat(result.categories()).containsExactly("Workbench");
        assertThat(result.categoryCounts()).containsEntry("Workbench", 5);
        assertThat(result.commandIds())
                .containsExactly(
                        "commands-surface-json",
                        "commands-profile-json",
                        "commands-index-json",
                        "commands-id-json",
                        "commands-contract-json-schema-id-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.command.discovery:v1:commands-discovery");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangCommandDiscoveryContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangCommandDiscoveryContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY);
                    assertThat(summary.count()).isEqualTo(5);
                });
    }

    @Test
    void filtersWorkbenchCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery"));

        assertThat(result.matchingCommands()).isEqualTo(4);
        assertThat(result.categories()).containsExactly("Workbench");
        assertThat(result.categoryCounts()).containsEntry("Workbench", 4);
        assertThat(result.commandIds())
                .containsExactly(
                        "workbench-surface-json",
                        "workbench-profile-json",
                        "workbench-command-json",
                        "workbench-contract-json-schema-id-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangWorkbenchContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangWorkbenchContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangWorkbenchContract.WORKBENCH_DISCOVERY);
                    assertThat(summary.count()).isEqualTo(4);
                });
    }

    @Test
    void filtersPlatformCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.platform.catalog:v1:profile-list"));

        assertThat(result.matchingCommands()).isEqualTo(2);
        assertThat(result.categories()).containsExactly("Platform");
        assertThat(result.categoryCounts()).containsEntry("Platform", 2);
        assertThat(result.commandIds())
                .containsExactly("profiles-json", "profiles-surface-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.platform.catalog:v1:profile-list");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangPlatformContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangPlatformContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangPlatformContract.PROFILE_LIST);
                    assertThat(summary.count()).isEqualTo(2);
                });
    }

    @Test
    void filtersReadinessCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.readiness:v1:readiness-aggregate"));

        assertThat(result.matchingCommands()).isEqualTo(2);
        assertThat(result.categories()).containsExactly("Platform");
        assertThat(result.categoryCounts()).containsEntry("Platform", 2);
        assertThat(result.commandIds())
                .containsExactly("status-readiness-json", "status-readiness-profile-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.readiness:v1:readiness-aggregate");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangReadinessContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangReadinessContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangReadinessContract.READINESS_AGGREGATE);
                    assertThat(summary.count()).isEqualTo(2);
                });
    }

    @Test
    void filtersStandardCatalogCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog"));

        assertThat(result.matchingCommands()).isEqualTo(1);
        assertThat(result.categories()).containsExactly("Standards");
        assertThat(result.categoryCounts()).containsEntry("Standards", 1);
        assertThat(result.commandIds()).containsExactly("standards-catalog-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangStandardCatalogContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangStandardCatalogContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangStandardCatalogContract.STANDARDS_CATALOG);
                    assertThat(summary.count()).isEqualTo(1);
                });
    }

    @Test
    void filtersStandardAlignmentCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health"));

        assertThat(result.matchingCommands()).isEqualTo(1);
        assertThat(result.categories()).containsExactly("Standards");
        assertThat(result.categoryCounts()).containsEntry("Standards", 1);
        assertThat(result.commandIds()).containsExactly("standards-health-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangStandardAlignmentContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangStandardAlignmentContract.VERSION);
                    assertThat(summary.envelope())
                            .isEqualTo(WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH);
                    assertThat(summary.count()).isEqualTo(1);
                });
    }

    @Test
    void filtersSkillCommandsByContractJsonSchemaId() {
        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forContractJsonSchemaId(
                        "urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery"));

        assertThat(result.matchingCommands()).isEqualTo(4);
        assertThat(result.categories()).containsExactly("Skills");
        assertThat(result.categoryCounts()).containsEntry("Skills", 4);
        assertThat(result.commandIds())
                .containsExactly(
                        "skills-list-json",
                        "skills-list-profile-json",
                        "skills-search-json",
                        "skills-search-profile-json");
        assertThat(result.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery");
        assertThat(result.contractSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.schema()).isEqualTo(WayangSkillContract.SCHEMA);
                    assertThat(summary.version()).isEqualTo(WayangSkillContract.VERSION);
                    assertThat(summary.envelope()).isEqualTo(WayangSkillContract.SKILL_DISCOVERY);
                    assertThat(summary.count()).isEqualTo(4);
                });
    }

    @Test
    void filtersCommandsByProductProfileSurface() {
        WorkbenchCommandQuery query = WorkbenchCommandQuery.forProfile(" low-code-agent ", "Runs", null);

        assertThat(query.profileId()).isEqualTo("low-code-agent");
        assertThat(query.surfaceId()).isNull();
        assertThat(query.resolvedSurfaceId()).isEqualTo("workflow-platform");

        WorkbenchCommandDiscovery result = discovery.commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                query);

        assertThat(result.query().profileId()).isEqualTo("low-code-agent");
        assertThat(result.matchingCommands()).isEqualTo(result.commands().size());
        assertThat(result.categories()).containsExactly("Runs");
        assertThat(result.commandIds())
                .contains("run-profile", "run-workflow-skill")
                .doesNotContain("run-assistant-surface", "workspace-inspect");
    }

    @Test
    void rejectsProfileSurfaceConflicts() {
        WorkbenchCommandQuery query = WorkbenchCommandQuery.of("assistant-agent", "low-code-agent", null, null);

        assertThatThrownBy(() -> discovery.discover(WayangWorkbenchCatalog.localCommands(), query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wayang product profile 'low-code-agent'")
                .hasMessageContaining("belongs to surface 'workflow-platform'")
                .hasMessageContaining("not 'assistant-agent'");
    }

    @Test
    void reportsUnknownCommandWithinFilteredCommandSet() {
        WorkbenchCommandQuery query = WorkbenchCommandQuery.of("assistant-agent", null, "workspace-inspect");

        assertThatThrownBy(() -> discovery.discover(WayangWorkbenchCatalog.localCommands(), query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang command id 'workspace-inspect'")
                .hasMessageContaining("run-assistant-surface")
                .hasMessageNotContaining("workspace-inspect,");
    }

    @Test
    void filtersWorkbenchModelAndRegeneratesPalette() {
        WayangWorkbenchModel workbench = new WayangWorkbenchModel(
                null,
                WayangProductCatalog.defaultSurfaces(),
                WayangWorkbenchCatalog.localCommandPalette(),
                WayangWorkbenchCatalog.localCommands(),
                java.util.List.of("next"));

        WayangWorkbenchModel filtered = discovery.filterWorkbench(
                workbench,
                WorkbenchCommandQuery.of("assistant-agent", "Runs", "run-session-context"));

        assertThat(filtered.status().productName()).isEqualTo("Wayang");
        assertThat(filtered.productSurfaces()).containsExactlyElementsOf(workbench.productSurfaces());
        assertThat(filtered.nextActions()).containsExactly("next");
        assertThat(filtered.commands())
                .singleElement()
                .satisfies(command -> assertThat(command.id()).isEqualTo("run-session-context"));
        assertThat(filtered.commandPalette())
                .containsExactly("run <task> --session <id> --user <id> --context rag.collection=<name>");
    }
}
