package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangWorkbenchCatalogTest {

    @Test
    void centralizesSharedWorkbenchCommands() {
        List<String> shared = WayangWorkbenchCatalog.sharedCommandPalette();

        assertThat(shared)
                .contains("status --readiness --json")
                .contains("readiness-profiles --json")
                .contains("readiness-profiles inspect <profile-id> --json")
                .contains("readiness-profiles --check --json")
                .contains("readiness-profiles policies --json")
                .contains("readiness-profiles sources --json")
                .contains("products --json")
                .contains("profiles --json")
                .contains("profiles --surface <surface-id> --json")
                .contains("profiles inspect <profile-id> --json")
                .contains("sdk-boundaries --json")
                .contains("sdk-boundaries <boundary-id> --json")
                .contains("spec template --surface coding-agent --output <file>")
                .contains("spec template --profile openclaw-agent")
                .contains("run <task> --json")
                .contains("run <task> --preflight --json")
                .contains("run <task> --profile <profile-id>")
                .contains("run <task> --profile <profile-id> --print-spec")
                .contains("run <task> --print-spec --output <file>")
                .contains("run status <run-id> --json")
                .contains("run inspect <run-id> --json")
                .contains("run events <run-id> --json")
                .contains("run events <run-id> --state completed --limit 20 --json")
                .contains("run events <run-id> --after-sequence 10 --limit 20 --json")
                .contains("run events <run-id> --follow --json")
                .contains("run events <run-id> --follow --follow-result --json")
                .contains("run events <run-id> --follow --follow-result-only --json")
                .contains("run events <run-id> --follow --follow-result-only --stats --json")
                .contains("run events <run-id> --stats --json")
                .contains("run list --state completed --limit 10 --json")
                .contains("run list --offset 10 --limit 10 --json")
                .contains("run stats --state completed --json")
                .contains("run list --tenant <id> --surface assistant-agent --json")
                .contains("run list --profile <profile-id> --json")
                .contains("run stats --profile <profile-id> --json")
                .contains("run wait <run-id> --timeout-seconds 30 --json")
                .contains("run cancel <run-id> --reason <text> --json")
                .contains("run <task> --workflow <gamelan-workflow> --skill <skill-id>")
                .contains("workbench --surface <surface-id> --json")
                .contains("workbench --profile <profile-id> --json")
                .contains("workbench --surface assistant-agent --category Runs --id run-session-context --json")
                .contains("workbench --contract-json-schema-id <schema-id> --json")
                .contains("commands --surface <surface-id> --json")
                .contains("commands --profile <profile-id> --json")
                .contains("commands --index --json")
                .contains("commands --category \"Run Specs\"")
                .contains("commands --id run-print-spec-output --json")
                .contains("commands --contract-json-schema-id <schema-id> --json")
                .contains("contracts --json")
                .contains("contracts --index --json")
                .contains("contracts --envelope <envelope> --schema-json")
                .contains("contracts --schema-bundle-json")
                .contains("contracts --check --json")
                .contains("contracts --coverage --json")
                .contains("contracts --command-id <command-id> --json")
                .contains("contracts --json-schema-id <schema-id> --json")
                .contains("contracts --domain <domain> --json")
                .contains("contracts --schema <schema-id> --json")
                .contains("contracts --envelope <envelope> --json")
                .contains("standards --json")
                .contains("standards --catalog --json")
                .contains("skills list --profile <profile-id> --json")
                .contains("skills search gamelan --profile <profile-id> --json")
                .contains("providers --json")
                .contains("providers list --module a2ui --json")
                .contains("providers search lifecycle --surface coding-agent --json")
                .contains("providers inspect storage.hybrid-persistence --json")
                .contains("workbench")
                .doesNotContain("run forget <run-id> --json")
                .doesNotContain("tui");
        assertThat(WayangWorkbenchCatalog.sharedCommands())
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-print-spec-output");
                    assertThat(command.category()).isEqualTo("Run Specs");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("profiles-inspect-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(WayangPlatformContract.PROFILE_DETAIL));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("sdk-boundaries-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.SDK_BOUNDARY_CATALOG));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("sdk-boundaries-inspect-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.SDK_BOUNDARY_DETAIL));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("status-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(WayangPlatformContract.PLATFORM_STATUS));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("status-readiness-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.readiness(
                                    WayangReadinessContract.READINESS_AGGREGATE));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_LIST));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-inspect-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_DETAIL));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-check-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_VALIDATION));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-policies-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-config-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("readiness-profiles-sources-json");
                    assertThat(command.category()).isEqualTo("Platform");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(
                                    WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("products-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(WayangPlatformContract.PRODUCT_CATALOG));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("profiles-surface-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.platform(WayangPlatformContract.PROFILE_LIST));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-profile");
                    assertThat(command.category()).isEqualTo("Runs");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-result-json");
                    assertThat(command.category()).isEqualTo("Runs");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.lifecycle("run-result"));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-dry-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.planning("run-preview"));
                    assertThat(command.contracts().get(0).jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-preflight-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.planning("run-preflight"));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-events-follow-result-json");
                    assertThat(command.contracts())
                            .containsExactly(
                                    WorkbenchCommandContract.lifecycle("run-events"),
                                    WorkbenchCommandContract.lifecycle("run-events-follow"));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-list-profile-json");
                    assertThat(command.category()).isEqualTo("Runs");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-profile-print-spec");
                    assertThat(command.category()).isEqualTo("Run Specs");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("commands-profile-json");
                    assertThat(command.category()).isEqualTo("Workbench");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.commandDiscovery(
                                    WayangCommandDiscoveryContract.COMMANDS_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("commands-index-json");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.commandDiscovery(
                                    WayangCommandDiscoveryContract.COMMANDS_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("workbench-profile-json");
                    assertThat(command.category()).isEqualTo("Workbench");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.workbenchDiscovery(
                                    WayangWorkbenchContract.WORKBENCH_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("workbench-contract-json-schema-id-json");
                    assertThat(command.category()).isEqualTo("Workbench");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.workbenchDiscovery(
                                    WayangWorkbenchContract.WORKBENCH_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("commands-contract-json-schema-id-json");
                    assertThat(command.category()).isEqualTo("Workbench");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.commandDiscovery(
                                    WayangCommandDiscoveryContract.COMMANDS_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("skills-list-profile-json");
                    assertThat(command.category()).isEqualTo("Skills");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.skill(WayangSkillContract.SKILL_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("skills-inspect-json");
                    assertThat(command.category()).isEqualTo("Skills");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.skill(WayangSkillContract.SKILL_DETAIL));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("skills-search-profile-json");
                    assertThat(command.category()).isEqualTo("Skills");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.skill(WayangSkillContract.SKILL_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("providers-json");
                    assertThat(command.category()).isEqualTo("Providers");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.providerCapability(
                                    WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("providers-inspect-json");
                    assertThat(command.category()).isEqualTo("Providers");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.providerCapability(
                                    WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("contracts-schema-json");
                    assertThat(command.category()).isEqualTo("Contracts");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("contracts-coverage-json");
                    assertThat(command.category()).isEqualTo("Contracts");
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.contractCoverage(
                                    WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("standards-health-json");
                    assertThat(command.category()).isEqualTo("Standards");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.standardAlignment(
                                    WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("standards-catalog-json");
                    assertThat(command.category()).isEqualTo("Standards");
                    assertThat(command.surfaceIds()).isEmpty();
                    assertThat(command.localOnly()).isFalse();
                    assertThat(command.contracts())
                            .containsExactly(WorkbenchCommandContract.standardCatalog(
                                    WayangStandardCatalogContract.STANDARDS_CATALOG));
                })
                .anySatisfy(command -> {
                    assertThat(command.id()).isEqualTo("run-workflow-skill");
                    assertThat(command.surfaceIds()).containsExactly("workflow-platform");
                });
    }

    @Test
    void localPaletteExtendsSharedCommandsWithTui() {
        assertThat(WayangWorkbenchCatalog.localCommandPalette())
                .containsAll(WayangWorkbenchCatalog.sharedCommandPalette())
                .contains("run store --json")
                .contains("run store --verify --json")
                .contains("run store --compact --dry-run --json")
                .contains("run store --compact --apply --json")
                .contains("run forget <run-id> --json")
                .contains("tui");
        assertThat(WayangWorkbenchCatalog.localCommands())
                .extracting(WorkbenchCommand::id)
                .contains(
                        "run-store-json",
                        "run-store-verify-json",
                        "run-store-compact-dry-run-json",
                        "run-store-compact-apply-json",
                        "run-forget-json",
                        "tui");
        assertThat(WayangWorkbenchCatalog.localCommands())
                .filteredOn(WorkbenchCommand::localOnly)
                .extracting(WorkbenchCommand::command)
                .containsExactly(
                        "run store --json",
                        "run store --verify --json",
                        "run store --compact --dry-run --json",
                        "run store --compact --apply --json",
                        "run forget <run-id> --json",
                        "tui");
        assertThat(WayangWorkbenchCatalog.findCommand(WayangWorkbenchCatalog.localCommands(), "run-store-json"))
                .hasValueSatisfying(command -> assertThat(command.contracts())
                        .containsExactly(WorkbenchCommandContract.lifecycle("run-store")));
        assertThat(WayangWorkbenchCatalog.findCommand(WayangWorkbenchCatalog.localCommands(), "run-store-verify-json"))
                .hasValueSatisfying(command -> assertThat(command.contracts())
                        .containsExactly(WorkbenchCommandContract.lifecycle("run-store-verification")));
        assertThat(WayangWorkbenchCatalog.findCommand(
                WayangWorkbenchCatalog.localCommands(),
                "run-store-compact-dry-run-json"))
                .hasValueSatisfying(command -> assertThat(command.contracts())
                        .containsExactly(WorkbenchCommandContract.lifecycle("run-store-compaction-preview")));
        assertThat(WayangWorkbenchCatalog.findCommand(
                WayangWorkbenchCatalog.localCommands(),
                "run-store-compact-apply-json"))
                .hasValueSatisfying(command -> assertThat(command.contracts())
                        .containsExactly(WorkbenchCommandContract.lifecycle("run-store-compaction")));
        assertThat(WayangWorkbenchCatalog.findCommand(WayangWorkbenchCatalog.localCommands(), "run-forget-json"))
                .hasValueSatisfying(command -> assertThat(command.contracts())
                        .containsExactly(WorkbenchCommandContract.lifecycle("run-forget")));
    }

    @Test
    void remotePaletteUsesSharedRemoteSafeCommands() {
        assertThat(WayangWorkbenchCatalog.remoteCommandPalette())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.sharedCommandPalette())
                .doesNotContain("tui");
        assertThat(WayangWorkbenchCatalog.remoteCommands())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.sharedCommands())
                .noneSatisfy(command -> assertThat(command.localOnly()).isTrue());
    }

    @Test
    void filtersCommandsByProductSurface() {
        assertThat(WayangWorkbenchCatalog.localCommandsForSurface("assistant-agent"))
                .extracting(WorkbenchCommand::id)
                .contains("status-json", "status-readiness-json", "run-assistant-surface", "run-session-context", "tui")
                .doesNotContain("workspace-inspect", "harness-plan", "run-workflow-skill");
        assertThat(WayangWorkbenchCatalog.remoteCommandsForSurface("workflow-platform"))
                .extracting(WorkbenchCommand::id)
                .contains("status-json", "status-readiness-json", "run-workflow-skill")
                .doesNotContain("tui", "workspace-inspect", "run-assistant-surface");
        assertThat(WayangWorkbenchCatalog.localCommandPaletteForSurface("coding-agent"))
                .contains("workspace --path <dir>", "harness --path <dir>", "tui")
                .doesNotContain("run <task> --surface assistant-agent");
    }

    @Test
    void rejectsUnknownSurfaceFilter() {
        assertThatThrownBy(() -> WayangWorkbenchCatalog.localCommandsForSurface("future-agent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'")
                .hasMessageContaining("coding-agent")
                .hasMessageContaining("assistant-agent");
    }

    @Test
    void filtersCommandsByCategoryAndId() {
        assertThat(WayangWorkbenchCatalog.commandsForCategory(WayangWorkbenchCatalog.localCommands(), "run specs"))
                .extracting(WorkbenchCommand::id)
                .contains(
                        "spec-validate",
                        "spec-template",
                        "spec-template-output",
                        "spec-template-profile",
                        "run-spec-dry-json",
                        "run-print-spec",
                        "run-print-spec-output",
                        "run-profile-print-spec")
                .doesNotContain("workspace-inspect", "tui");
        assertThat(WayangWorkbenchCatalog.commandsForCategory(WayangWorkbenchCatalog.localCommands(), "contracts"))
                .extracting(WorkbenchCommand::id)
                .containsExactly(
                        "contracts-json",
                        "contracts-index-json",
                        "contracts-envelope-schema-json",
                        "contracts-schema-bundle-json",
                        "contracts-check-json",
                        "contracts-coverage-json",
                        "contracts-command-json",
                        "contracts-json-schema-id-json",
                        "contracts-domain-json",
                        "contracts-schema-json",
                        "contracts-envelope-json");
        assertThat(WayangWorkbenchCatalog.commandsForCategory(WayangWorkbenchCatalog.localCommands(), "standards"))
                .extracting(WorkbenchCommand::id)
                .containsExactly("standards-health-json", "standards-catalog-json");
        assertThat(WayangWorkbenchCatalog.commandsForCategory(WayangWorkbenchCatalog.localCommands(), "providers"))
                .extracting(WorkbenchCommand::id)
                .containsExactly(
                        "providers-json",
                        "providers-list-json",
                        "providers-search-json",
                        "providers-inspect-json");

        assertThat(WayangWorkbenchCatalog.commandsForId(WayangWorkbenchCatalog.localCommands(), "run-print-spec-output"))
                .singleElement()
                .satisfies(command -> {
                    assertThat(command.title()).isEqualTo("Save Resolved Spec");
                    assertThat(command.command()).isEqualTo("run <task> --print-spec --output <file>");
                });

        assertThat(WayangWorkbenchCatalog.findCommand(WayangWorkbenchCatalog.localCommands(), "tui"))
                .hasValueSatisfying(command -> assertThat(command.localOnly()).isTrue());
        assertThat(WayangWorkbenchCatalog.knownCommandCategories(WayangWorkbenchCatalog.localCommands()))
                .containsExactly(
                        "Platform",
                        "Context",
                        "Harness",
                        "Run Specs",
                        "Runs",
                        "Contracts",
                        "Standards",
                        "Workbench",
                        "Skills",
                        "Providers");
    }

    @Test
    void rejectsUnknownCommandCategoryAndId() {
        assertThatThrownBy(() -> WayangWorkbenchCatalog.commandsForCategory(
                WayangWorkbenchCatalog.localCommands(),
                "Future"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang command category 'Future'")
                .hasMessageContaining("Run Specs")
                .hasMessageContaining("Workbench");

        assertThatThrownBy(() -> WayangWorkbenchCatalog.commandsForId(
                WayangWorkbenchCatalog.localCommands(),
                "future-command"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang command id 'future-command'")
                .hasMessageContaining("run-print-spec-output")
                .hasMessageContaining("tui");
    }

    @Test
    void palettesAreImmutable() {
        assertThatThrownBy(() -> WayangWorkbenchCatalog.localCommandPalette().add("future"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> WayangWorkbenchCatalog.remoteCommandPalette().add("future"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> WayangWorkbenchCatalog.localCommands().add(WayangWorkbenchCatalog.sharedCommands().get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void workbenchModelKeepsLegacyPaletteConstructorCompatible() {
        WayangWorkbenchModel model = new WayangWorkbenchModel(
                null,
                List.of(),
                List.of("status --json"),
                List.of());

        assertThat(model.commandPalette()).containsExactly("status --json");
        assertThat(model.commands())
                .singleElement()
                .satisfies(command -> {
                    assertThat(command.command()).isEqualTo("status --json");
                    assertThat(command.category()).isEqualTo("General");
                });
    }
}
