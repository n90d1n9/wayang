package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WayangWorkbenchCatalog {

    private static final List<WorkbenchCommand> SHARED_COMMANDS = List.of(
            command(
                    "status-json",
                    "Status JSON",
                    "status --json",
                    "Platform",
                    "Render platform boundary and adapter status as machine-readable JSON.",
                    platform(WayangPlatformContract.PLATFORM_STATUS)),
            command(
                    "status-readiness-json",
                    "Platform Readiness JSON",
                    "status --readiness --json",
                    "Platform",
                    "Render aggregate production readiness as machine-readable JSON.",
                    readiness(WayangReadinessContract.READINESS_AGGREGATE)),
            command(
                    "status-readiness-profile-json",
                    "Platform Readiness Profile JSON",
                    "status --readiness-profile <profile-id> --json",
                    "Platform",
                    "Render a selected platform readiness profile as machine-readable JSON.",
                    readiness(WayangReadinessContract.READINESS_AGGREGATE)),
            command(
                    "readiness-profiles-json",
                    "Readiness Profiles JSON",
                    "readiness-profiles --json",
                    "Platform",
                    "Render available platform readiness profiles and component bindings as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_LIST)),
            command(
                    "readiness-profiles-inspect-json",
                    "Inspect Readiness Profile JSON",
                    "readiness-profiles inspect <profile-id> --json",
                    "Platform",
                    "Render one platform readiness profile and component binding as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_DETAIL)),
            command(
                    "readiness-profiles-check-json",
                    "Check Readiness Profiles JSON",
                    "readiness-profiles --check --json",
                    "Platform",
                    "Validate platform readiness profile bindings as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_VALIDATION)),
            command(
                    "readiness-profiles-policies-json",
                    "Readiness Profile Validation Policies JSON",
                    "readiness-profiles policies --json",
                    "Platform",
                    "Render supported readiness profile validation policies as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST)),
            command(
                    "readiness-profiles-config-json",
                    "Readiness Profile Registry Config JSON",
                    "readiness-profiles config --json",
                    "Platform",
                    "Render readiness profile registry configuration diagnostics as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS)),
            command(
                    "readiness-profiles-sources-json",
                    "Readiness Profile Sources JSON",
                    "readiness-profiles sources --json",
                    "Platform",
                    "Render readiness profile registry source resolution and fallback status as JSON.",
                    platform(WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION)),
            command(
                    "products",
                    "Product Surfaces",
                    "products",
                    "Platform",
                    "List product surfaces backed by the shared Wayang agent engine."),
            command(
                    "products-json",
                    "Product Surfaces JSON",
                    "products --json",
                    "Platform",
                    "Render product surfaces, policies, and profiles as machine-readable JSON.",
                    platform(WayangPlatformContract.PRODUCT_CATALOG)),
            command(
                    "profiles-list",
                    "Product Profiles",
                    "profiles",
                    "Platform",
                    "List reusable product profiles backed by the shared Wayang agent engine."),
            command(
                    "profiles-json",
                    "Product Profiles JSON",
                    "profiles --json",
                    "Platform",
                    "Render reusable product profiles as machine-readable JSON.",
                    platform(WayangPlatformContract.PROFILE_LIST)),
            command(
                    "profiles-surface-json",
                    "Surface Profiles JSON",
                    "profiles --surface <surface-id> --json",
                    "Platform",
                    "Render reusable product profiles filtered to one product surface.",
                    platform(WayangPlatformContract.PROFILE_LIST)),
            command(
                    "profiles-inspect-json",
                    "Inspect Product Profile JSON",
                    "profiles inspect <profile-id> --json",
                    "Platform",
                    "Render one reusable product profile as machine-readable JSON.",
                    platform(WayangPlatformContract.PROFILE_DETAIL)),
            command(
                    "sdk-boundaries",
                    "SDK Boundaries",
                    "sdk-boundaries",
                    "Platform",
                    "List SDK ownership boundaries for package and API separation planning."),
            command(
                    "sdk-boundaries-json",
                    "SDK Boundaries JSON",
                    "sdk-boundaries --json",
                    "Platform",
                    "Render SDK ownership boundaries as machine-readable JSON.",
                    platform(WayangPlatformContract.SDK_BOUNDARY_CATALOG)),
            command(
                    "sdk-boundaries-inspect-json",
                    "Inspect SDK Boundary JSON",
                    "sdk-boundaries <boundary-id> --json",
                    "Platform",
                    "Render one SDK ownership boundary as machine-readable JSON.",
                    platform(WayangPlatformContract.SDK_BOUNDARY_DETAIL)),
            command(
                    "workspace-inspect",
                    "Inspect Workspace",
                    "workspace --path <dir>",
                    "Context",
                    "Inspect repository context for coding-agent runs.",
                    "coding-agent"),
            command(
                    "harness-plan",
                    "Plan Harness",
                    "harness --path <dir>",
                    "Harness",
                    "Plan verification checks without executing them.",
                    "coding-agent"),
            command(
                    "spec-validate",
                    "Validate Run Spec",
                    "spec validate --path <file>",
                    "Run Specs",
                    "Validate a portable Wayang run spec through the preview contract."),
            command(
                    "spec-template",
                    "Print Run Spec Template",
                    "spec template --surface coding-agent",
                    "Run Specs",
                    "Print a starter run spec for a product surface."),
            command(
                    "spec-template-output",
                    "Save Run Spec Template",
                    "spec template --surface coding-agent --output <file>",
                    "Run Specs",
                    "Write a starter run spec to a UTF-8 file."),
            command(
                    "spec-template-profile",
                    "Print Profile Run Spec Template",
                    "spec template --profile openclaw-agent",
                    "Run Specs",
                    "Print a starter run spec from a reusable product profile."),
            command(
                    "run-workspace",
                    "Run With Workspace",
                    "run <task> --workspace <dir>",
                    "Runs",
                    "Prepare a coding-agent run with inspected workspace context.",
                    "coding-agent"),
            command(
                    "run-harness",
                    "Run With Harness",
                    "run <task> --harness",
                    "Runs",
                    "Prepare a run with planned verification context.",
                    "coding-agent"),
            command(
                    "run-result-json",
                    "Run Result JSON",
                    "run <task> --json",
                    "Runs",
                    "Submit a run and render the immediate run result envelope.",
                    lifecycle(AgentRunLifecycleContract.RUN_RESULT)),
            command(
                    "run-preflight-json",
                    "Run Preflight JSON",
                    "run <task> --preflight --json",
                    "Runs",
                    "Render readiness checks before preparing a run.",
                    planning(AgentRunPlanningContract.RUN_PREFLIGHT)),
            command(
                    "run-dry-json",
                    "Dry Run JSON",
                    "run <task> --dry-run --json",
                    "Runs",
                    "Render the normalized run preview as JSON.",
                    planning(AgentRunPlanningContract.RUN_PREVIEW)),
            command(
                    "run-profile",
                    "Run With Product Profile",
                    "run <task> --profile <profile-id>",
                    "Runs",
                    "Prepare a run from reusable product profile defaults."),
            command(
                    "run-spec-dry-json",
                    "Dry Run Saved Spec",
                    "run --spec <file> --dry-run --json",
                    "Run Specs",
                    "Preview a saved run spec without submitting a run.",
                    planning(AgentRunPlanningContract.RUN_PREVIEW)),
            command(
                    "run-print-spec",
                    "Print Resolved Spec",
                    "run <task> --print-spec",
                    "Run Specs",
                    "Print the resolved run request as deterministic properties."),
            command(
                    "run-print-spec-output",
                    "Save Resolved Spec",
                    "run <task> --print-spec --output <file>",
                    "Run Specs",
                    "Write the resolved run request to a UTF-8 properties file."),
            command(
                    "run-profile-print-spec",
                    "Print Profile Resolved Spec",
                    "run <task> --profile <profile-id> --print-spec",
                    "Run Specs",
                    "Print a resolved run spec after applying product profile defaults."),
            command(
                    "run-prompt-system-files",
                    "Run From Prompt Files",
                    "run --prompt-file <file> --system-file <file>",
                    "Runs",
                    "Prepare a run from prompt and system prompt files."),
            command(
                    "run-assistant-surface",
                    "Run Assistant Surface",
                    "run <task> --surface assistant-agent",
                    "Runs",
                    "Route a run through the assistant-agent product surface.",
                    "assistant-agent"),
            command(
                    "run-session-context",
                    "Run With Session Context",
                    "run <task> --session <id> --user <id> --context rag.collection=<name>",
                    "Runs",
                    "Attach session, user, and namespaced context metadata.",
                    "assistant-agent"),
            command(
                    "run-tenant-model",
                    "Run With Tenant And Model",
                    "run <task> --tenant <id> --model <gollek-model>",
                    "Runs",
                    "Override SDK tenant and Gollek model defaults."),
            command(
                    "run-status-json",
                    "Run Status JSON",
                    "run status <run-id> --json",
                    "Runs",
                    "Render a lifecycle status snapshot for a run id.",
                    lifecycle(AgentRunLifecycleContract.RUN_STATUS)),
            command(
                    "run-inspect-json",
                    "Run Inspection JSON",
                    "run inspect <run-id> --json",
                    "Runs",
                    "Render a run status snapshot and lifecycle event timeline together.",
                    lifecycle(AgentRunLifecycleContract.RUN_INSPECT)),
            command(
                    "run-events-json",
                    "Run Events JSON",
                    "run events <run-id> --json",
                    "Runs",
                    "Render the lifecycle event timeline for a run id.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS)),
            command(
                    "run-events-filter-json",
                    "Filtered Run Events JSON",
                    "run events <run-id> --state completed --limit 20 --json",
                    "Runs",
                    "Render a bounded lifecycle event timeline filtered by state or type.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS)),
            command(
                    "run-events-cursor-json",
                    "Cursor Run Events JSON",
                    "run events <run-id> --after-sequence 10 --limit 20 --json",
                    "Runs",
                    "Render lifecycle events after a sequence cursor for polling UIs.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS)),
            command(
                    "run-events-follow-json",
                    "Follow Run Events JSON",
                    "run events <run-id> --follow --json",
                    "Runs",
                    "Poll lifecycle events with cursor advancement until a terminal event or max polls.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS)),
            command(
                    "run-events-follow-result-json",
                    "Follow Run Events Result JSON",
                    "run events <run-id> --follow --follow-result --json",
                    "Runs",
                    "Poll lifecycle events and render a final follow result envelope for product shells.",
                    lifecycle(
                            AgentRunLifecycleContract.RUN_EVENTS,
                            AgentRunLifecycleContract.RUN_EVENTS_FOLLOW)),
            command(
                    "run-events-follow-result-only-json",
                    "Follow Run Events Result Only JSON",
                    "run events <run-id> --follow --follow-result-only --json",
                    "Runs",
                    "Poll lifecycle events and render only the final follow result envelope.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS_FOLLOW)),
            command(
                    "run-events-follow-result-only-stats-json",
                    "Follow Run Events Result Only Stats JSON",
                    "run events <run-id> --follow --follow-result-only --stats --json",
                    "Runs",
                    "Poll lifecycle events and render only a rowless final follow result envelope.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS_FOLLOW)),
            command(
                    "run-events-stats-json",
                    "Run Event Stats JSON",
                    "run events <run-id> --stats --json",
                    "Runs",
                    "Render lifecycle event cursor and summary envelopes without event rows.",
                    lifecycle(AgentRunLifecycleContract.RUN_EVENTS_STATS)),
            command(
                    "run-list-json",
                    "Run List JSON",
                    "run list --state completed --limit 10 --json",
                    "Runs",
                    "Render filtered lifecycle status snapshots for this SDK session.",
                    lifecycle(AgentRunLifecycleContract.RUN_LIST)),
            command(
                    "run-list-page-json",
                    "Paged Run List JSON",
                    "run list --offset 10 --limit 10 --json",
                    "Runs",
                    "Render one paged lifecycle history window with nextOffset and hasMore.",
                    lifecycle(AgentRunLifecycleContract.RUN_LIST)),
            command(
                    "run-stats-json",
                    "Run Stats JSON",
                    "run stats --state completed --json",
                    "Runs",
                    "Render lifecycle history page and summary envelopes without run rows.",
                    lifecycle(AgentRunLifecycleContract.RUN_STATS)),
            command(
                    "run-list-filter-json",
                    "Filtered Run List JSON",
                    "run list --tenant <id> --surface assistant-agent --json",
                    "Runs",
                    "Render lifecycle status snapshots filtered by tenant, session, or surface.",
                    lifecycle(AgentRunLifecycleContract.RUN_LIST)),
            command(
                    "run-list-profile-json",
                    "Profile Run List JSON",
                    "run list --profile <profile-id> --json",
                    "Runs",
                    "Render lifecycle status snapshots filtered by reusable product profile.",
                    lifecycle(AgentRunLifecycleContract.RUN_LIST)),
            command(
                    "run-stats-profile-json",
                    "Profile Run Stats JSON",
                    "run stats --profile <profile-id> --json",
                    "Runs",
                    "Render lifecycle summary envelopes filtered by reusable product profile.",
                    lifecycle(AgentRunLifecycleContract.RUN_STATS)),
            command(
                    "run-wait-json",
                    "Wait Run JSON",
                    "run wait <run-id> --timeout-seconds 30 --json",
                    "Runs",
                    "Poll run status until the run reaches a terminal lifecycle state.",
                    lifecycle(AgentRunLifecycleContract.RUN_WAIT)),
            command(
                    "run-cancel-json",
                    "Cancel Run JSON",
                    "run cancel <run-id> --reason <text> --json",
                    "Runs",
                    "Request cancellation for a non-terminal run.",
                    lifecycle(AgentRunLifecycleContract.RUN_CANCEL)),
            command(
                    "run-workflow-skill",
                    "Run Workflow Skill",
                    "run <task> --workflow <gamelan-workflow> --skill <skill-id>",
                    "Runs",
                    "Attach a Gamelan workflow id and allowed skill.",
                    "workflow-platform"),
            command(
                    "contracts-json",
                    "Contract Catalog JSON",
                    "contracts --json",
                    "Contracts",
                    "Render the SDK-owned JSON contract catalog for product shells."),
            command(
                    "contracts-index-json",
                    "Contract Index JSON",
                    "contracts --index --json",
                    "Contracts",
                    "Render contract discovery metadata and facets without descriptors."),
            command(
                    "contracts-envelope-schema-json",
                    "Contract Envelope Schema JSON",
                    "contracts --envelope <envelope> --schema-json",
                    "Contracts",
                    "Render JSON Schema for one contract envelope."),
            command(
                    "contracts-schema-bundle-json",
                    "Contract Schema Bundle JSON",
                    "contracts --schema-bundle-json",
                    "Contracts",
                    "Render JSON Schema documents for all matching contracts."),
            command(
                    "contracts-check-json",
                    "Contract Integrity JSON",
                    "contracts --check --json",
                    "Contracts",
                    "Validate bidirectional links between JSON contracts and command ids."),
            command(
                    "contracts-coverage-json",
                    "Contract Command Coverage JSON",
                    "contracts --coverage --json",
                    "Contracts",
                    "Render command coverage for all SDK-owned JSON contracts.",
                    contractCoverage(WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE)),
            command(
                    "contracts-command-json",
                    "Command Contract JSON",
                    "contracts --command-id <command-id> --json",
                    "Contracts",
                    "Render contract descriptors linked to one stable command id."),
            command(
                    "contracts-json-schema-id-json",
                    "JSON Schema ID Contracts JSON",
                    "contracts --json-schema-id <schema-id> --json",
                    "Contracts",
                    "Render contract descriptors for one JSON Schema id."),
            command(
                    "contracts-domain-json",
                    "Domain Contracts JSON",
                    "contracts --domain <domain> --json",
                    "Contracts",
                    "Render contract descriptors for one contract domain."),
            command(
                    "contracts-schema-json",
                    "Contract Schema JSON",
                    "contracts --schema <schema-id> --json",
                    "Contracts",
                    "Render contract descriptors for one schema family."),
            command(
                    "contracts-envelope-json",
                    "Contract Envelope JSON",
                    "contracts --envelope <envelope> --json",
                    "Contracts",
                    "Render one JSON envelope descriptor from the contract catalog."),
            command(
                    "standards-health-json",
                    "Standards Health JSON",
                    "standards --json",
                    "Standards",
                    "Render standard-alignment readiness health with policy, drift, and provider diagnostics.",
                    standardAlignment(WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH)),
            command(
                    "standards-catalog-json",
                    "Standards Catalog JSON",
                    "standards --catalog --json",
                    "Standards",
                    "Render known interoperability standards with pinned versions, aliases, bindings, and spec URLs.",
                    standardCatalog(WayangStandardCatalogContract.STANDARDS_CATALOG)),
            command(
                    "workbench-surface-json",
                    "Surface Workbench JSON",
                    "workbench --surface <surface-id> --json",
                    "Workbench",
                    "Render commands filtered to one product surface.",
                    workbenchDiscovery(WayangWorkbenchContract.WORKBENCH_DISCOVERY)),
            command(
                    "workbench-profile-json",
                    "Profile Workbench JSON",
                    "workbench --profile <profile-id> --json",
                    "Workbench",
                    "Render commands filtered through one reusable product profile.",
                    workbenchDiscovery(WayangWorkbenchContract.WORKBENCH_DISCOVERY)),
            command(
                    "workbench-command-json",
                    "Command Workbench JSON",
                    "workbench --surface assistant-agent --category Runs --id run-session-context --json",
                    "Workbench",
                    "Render the full workbench payload with a narrowed command model.",
                    workbenchDiscovery(WayangWorkbenchContract.WORKBENCH_DISCOVERY)),
            command(
                    "workbench-contract-json-schema-id-json",
                    "Contract Workbench JSON",
                    "workbench --contract-json-schema-id <schema-id> --json",
                    "Workbench",
                    "Render the full workbench payload with commands for one contract JSON Schema id.",
                    workbenchDiscovery(WayangWorkbenchContract.WORKBENCH_DISCOVERY)),
            command(
                    "commands-surface-json",
                    "Surface Commands JSON",
                    "commands --surface <surface-id> --json",
                    "Workbench",
                    "Render only the command catalog for one product surface.",
                    commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)),
            command(
                    "commands-profile-json",
                    "Profile Commands JSON",
                    "commands --profile <profile-id> --json",
                    "Workbench",
                    "Render only the command catalog filtered through one reusable product profile.",
                    commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)),
            command(
                    "commands-index-json",
                    "Command Index JSON",
                    "commands --index --json",
                    "Workbench",
                    "Render command discovery metadata without full command entries.",
                    commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)),
            command(
                    "commands-category",
                    "Category Commands",
                    "commands --category \"Run Specs\"",
                    "Workbench",
                    "Render only one command category for command palettes and help surfaces."),
            command(
                    "commands-id-json",
                    "Command Detail JSON",
                    "commands --id run-print-spec-output --json",
                    "Workbench",
                    "Render one stable command entry as compact JSON.",
                    commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)),
            command(
                    "commands-contract-json-schema-id-json",
                    "Contract Commands JSON",
                    "commands --contract-json-schema-id <schema-id> --json",
                    "Workbench",
                    "Render commands that produce one contract JSON Schema id.",
                    commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)),
            command(
                    "skills-list-json",
                    "Skill List JSON",
                    "skills list --surface assistant-agent --json",
                    "Skills",
                    "Render SDK-owned dynamic skill capability metadata.",
                    skill(WayangSkillContract.SKILL_DISCOVERY)),
            command(
                    "skills-list-profile-json",
                    "Profile Skill List JSON",
                    "skills list --profile <profile-id> --json",
                    "Skills",
                    "Render SDK-owned skill capabilities from a reusable product profile.",
                    skill(WayangSkillContract.SKILL_DISCOVERY)),
            command(
                    "skills-inspect-json",
                    "Skill Inspect JSON",
                    "skills inspect rag.retrieve --json",
                    "Skills",
                    "Render one stable skill capability contract.",
                    skill(WayangSkillContract.SKILL_DETAIL)),
            command(
                    "skills-search-json",
                    "Skill Search JSON",
                    "skills search rag --surface assistant-agent --json",
                    "Skills",
                    "Search SDK skill capabilities for command palettes and product shells.",
                    skill(WayangSkillContract.SKILL_DISCOVERY)),
            command(
                    "skills-search-profile-json",
                    "Profile Skill Search JSON",
                    "skills search gamelan --profile <profile-id> --json",
                    "Skills",
                    "Search SDK skill capabilities filtered through one reusable product profile.",
                    skill(WayangSkillContract.SKILL_DISCOVERY)),
            command(
                    "providers-json",
                    "Provider Capabilities JSON",
                    "providers --json",
                    "Providers",
                    "Render cross-module provider capability discovery metadata.",
                    providerCapability(WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY)),
            command(
                    "providers-list-json",
                    "Provider Capability List JSON",
                    "providers list --module a2ui --json",
                    "Providers",
                    "Render provider capabilities filtered by module, provider, standard, or surface.",
                    providerCapability(WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY)),
            command(
                    "providers-search-json",
                    "Provider Capability Search JSON",
                    "providers search lifecycle --surface coding-agent --json",
                    "Providers",
                    "Search provider capabilities across standards, modules, and provider metadata.",
                    providerCapability(WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY)),
            command(
                    "providers-inspect-json",
                    "Provider Capability Inspect JSON",
                    "providers inspect storage.hybrid-persistence --json",
                    "Providers",
                    "Render one stable provider capability detail contract.",
                    providerCapability(WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL)),
            command(
                    "workbench",
                    "Workbench",
                    "workbench",
                    "Workbench",
                    "Render the SDK-owned workbench model."));

    private static final List<WorkbenchCommand> LOCAL_COMMANDS = append(
            SHARED_COMMANDS,
            WorkbenchCommand.local(
                    "run-store-json",
                    "Run Store JSON",
                    "run store --json",
                    "Runs",
                    "Inspect the configured local run-store backend, snapshot, and retention policy.",
                    List.of(),
                    lifecycle(AgentRunLifecycleContract.RUN_STORE)),
            WorkbenchCommand.local(
                    "run-store-verify-json",
                    "Verify Run Store JSON",
                    "run store --verify --json",
                    "Runs",
                    "Verify the configured local run-store snapshot without mutating it.",
                    List.of(),
                    lifecycle(AgentRunLifecycleContract.RUN_STORE_VERIFICATION)),
            WorkbenchCommand.local(
                    "run-store-compact-dry-run-json",
                    "Preview Run Store Compaction JSON",
                    "run store --compact --dry-run --json",
                    "Runs",
                    "Preview retention compaction for the configured local run-store without mutating it.",
                    List.of(),
                    lifecycle(AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW)),
            WorkbenchCommand.local(
                    "run-store-compact-apply-json",
                    "Apply Run Store Compaction JSON",
                    "run store --compact --apply --json",
                    "Runs",
                    "Apply retention compaction to the configured local run-store.",
                    List.of(),
                    lifecycle(AgentRunLifecycleContract.RUN_STORE_COMPACTION)),
            WorkbenchCommand.local(
                    "run-forget-json",
                    "Forget Run JSON",
                    "run forget <run-id> --json",
                    "Runs",
                    "Forget a locally recorded run status snapshot.",
                    List.of(),
                    lifecycle(AgentRunLifecycleContract.RUN_FORGET)),
            WorkbenchCommand.local(
                    "tui",
                    "Terminal UI",
                    "tui",
                    "Workbench",
                    "Open the Tamboui-powered local terminal dashboard.",
                    List.of()));

    private WayangWorkbenchCatalog() {
    }

    public static List<WorkbenchCommand> sharedCommands() {
        return SHARED_COMMANDS;
    }

    public static List<WorkbenchCommand> localCommands() {
        return LOCAL_COMMANDS;
    }

    public static List<WorkbenchCommand> remoteCommands() {
        return SHARED_COMMANDS;
    }

    public static List<WorkbenchCommand> sharedCommandsForSurface(String surfaceId) {
        return commandsForSurface(SHARED_COMMANDS, surfaceId);
    }

    public static List<WorkbenchCommand> localCommandsForSurface(String surfaceId) {
        return commandsForSurface(LOCAL_COMMANDS, surfaceId);
    }

    public static List<WorkbenchCommand> remoteCommandsForSurface(String surfaceId) {
        return commandsForSurface(SHARED_COMMANDS, surfaceId);
    }

    public static List<WorkbenchCommand> commandsForSurface(List<WorkbenchCommand> commands, String surfaceId) {
        return WorkbenchCommandIndex.of(commands).commandsForSurface(surfaceId);
    }

    public static List<WorkbenchCommand> commandsForCategory(List<WorkbenchCommand> commands, String category) {
        return WorkbenchCommandIndex.of(commands).commandsForCategory(category);
    }

    public static List<WorkbenchCommand> commandsForId(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCommandIndex.of(commands).commandsForId(commandId);
    }

    public static Optional<WorkbenchCommand> findCommand(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCommandIndex.of(commands).findCommand(commandId);
    }

    public static List<String> knownCommandIds(List<WorkbenchCommand> commands) {
        return WorkbenchCommandIndex.of(commands).commandIds();
    }

    public static List<String> knownCommandCategories(List<WorkbenchCommand> commands) {
        return WorkbenchCommandIndex.of(commands).categories();
    }

    public static List<String> sharedCommandPalette() {
        return palette(SHARED_COMMANDS);
    }

    public static List<String> localCommandPalette() {
        return palette(LOCAL_COMMANDS);
    }

    public static List<String> remoteCommandPalette() {
        return palette(SHARED_COMMANDS);
    }

    public static List<String> sharedCommandPaletteForSurface(String surfaceId) {
        return palette(sharedCommandsForSurface(surfaceId));
    }

    public static List<String> localCommandPaletteForSurface(String surfaceId) {
        return palette(localCommandsForSurface(surfaceId));
    }

    public static List<String> remoteCommandPaletteForSurface(String surfaceId) {
        return palette(remoteCommandsForSurface(surfaceId));
    }

    private static WorkbenchCommand command(
            String id,
            String title,
            String command,
            String category,
            String description,
            String... surfaceIds) {
        return command(id, title, command, category, description, List.of(), surfaceIds);
    }

    private static WorkbenchCommand command(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<WorkbenchCommandContract> contracts,
            String... surfaceIds) {
        return WorkbenchCommand.shared(id, title, command, category, description, List.of(surfaceIds), contracts);
    }

    private static List<WorkbenchCommandContract> lifecycle(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::lifecycle)
                .toList();
    }

    private static List<WorkbenchCommandContract> planning(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::planning)
                .toList();
    }

    private static List<WorkbenchCommandContract> readiness(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::readiness)
                .toList();
    }

    private static List<WorkbenchCommandContract> platform(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::platform)
                .toList();
    }

    private static List<WorkbenchCommandContract> skill(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::skill)
                .toList();
    }

    private static List<WorkbenchCommandContract> providerCapability(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::providerCapability)
                .toList();
    }

    private static List<WorkbenchCommandContract> contractCoverage(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::contractCoverage)
                .toList();
    }

    private static List<WorkbenchCommandContract> standardCatalog(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::standardCatalog)
                .toList();
    }

    private static List<WorkbenchCommandContract> standardAlignment(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::standardAlignment)
                .toList();
    }

    private static List<WorkbenchCommandContract> commandDiscovery(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::commandDiscovery)
                .toList();
    }

    private static List<WorkbenchCommandContract> workbenchDiscovery(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::workbenchDiscovery)
                .toList();
    }

    private static List<String> palette(List<WorkbenchCommand> commands) {
        return commands.stream()
                .map(WorkbenchCommand::command)
                .toList();
    }

    private static List<WorkbenchCommand> append(List<WorkbenchCommand> baseCommands, WorkbenchCommand... additions) {
        List<WorkbenchCommand> commands = new ArrayList<>(baseCommands);
        commands.addAll(List.of(additions));
        return List.copyOf(commands);
    }
}
