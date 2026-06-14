package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public final class WayangContractCatalog {

    private static final List<WayangContractDescriptor> CONTRACTS = List.of(
            lifecycle(
                    AgentRunLifecycleContract.RUN_RESULT,
                    "Immediate run result envelope with lifecycle handle, outcome, answer, steps, and metadata.",
                    List.of("run-result-json"),
                    "run <prompt> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STATUS,
                    "Lifecycle status snapshot for one run id.",
                    List.of("run-status-json"),
                    "run status <run-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_EVENTS,
                    "Lifecycle event timeline rows with cursor and summary.",
                    List.of(
                            "run-events-json",
                            "run-events-filter-json",
                            "run-events-cursor-json",
                            "run-events-follow-json",
                            "run-events-follow-result-json"),
                    "run events <run-id> --json",
                    "run events <run-id> --state completed --limit 20 --json",
                    "run events <run-id> --after-sequence 10 --limit 20 --json",
                    "run events <run-id> --follow --json",
                    "run events <run-id> --follow --follow-result --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_EVENTS_STATS,
                    "Rowless lifecycle event cursor and summary envelope.",
                    List.of("run-events-stats-json"),
                    "run events <run-id> --stats --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_EVENTS_FOLLOW,
                    "Bounded lifecycle follow result with terminal routing metadata.",
                    List.of(
                            "run-events-follow-result-json",
                            "run-events-follow-result-only-json",
                            "run-events-follow-result-only-stats-json"),
                    "run events <run-id> --follow --follow-result --json",
                    "run events <run-id> --follow --follow-result-only --json",
                    "run events <run-id> --follow --follow-result-only --stats --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_INSPECT,
                    "Combined run status and event timeline envelope.",
                    List.of("run-inspect-json"),
                    "run inspect <run-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_LIST,
                    "Paged lifecycle status history with summary facets.",
                    List.of(
                            "run-list-json",
                            "run-list-page-json",
                            "run-list-filter-json",
                            "run-list-profile-json"),
                    "run list --json",
                    "run list --offset 10 --limit 10 --json",
                    "run list --tenant <id> --surface assistant-agent --json",
                    "run list --profile <profile-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STATS,
                    "Rowless lifecycle history page and summary envelope.",
                    List.of("run-stats-json", "run-stats-profile-json"),
                    "run stats --json",
                    "run stats --profile <profile-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_WAIT,
                    "Run wait result after polling for terminal status.",
                    List.of("run-wait-json"),
                    "run wait <run-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_CANCEL,
                    "Run cancellation request result.",
                    List.of("run-cancel-json"),
                    "run cancel <run-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_FORGET,
                    "Local run status forget result.",
                    List.of("run-forget-json"),
                    "run forget <run-id> --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STORE,
                    "Run-store backend, snapshot, and retention diagnostics for operator surfaces.",
                    List.of("run-store-json"),
                    "run store --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STORE_VERIFICATION,
                    "Run-store verification report with pass/fail status, issues, and diagnostics.",
                    List.of("run-store-verify-json"),
                    "run store --verify --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW,
                    "Dry-run run-store retention compaction preview for operator surfaces.",
                    List.of("run-store-compact-dry-run-json"),
                    "run store --compact --dry-run --json"),
            lifecycle(
                    AgentRunLifecycleContract.RUN_STORE_COMPACTION,
                    "Explicit run-store retention compaction result for operator surfaces.",
                    List.of("run-store-compact-apply-json"),
                    "run store --compact --apply --json"),
            planning(
                    AgentRunPlanningContract.RUN_PREFLIGHT,
                    "Run readiness preflight result before preparation.",
                    List.of("run-preflight-json"),
                    "run <prompt> --preflight --json"),
            planning(
                    AgentRunPlanningContract.RUN_PREVIEW,
                    "Normalized dry-run preview of the prepared run request.",
                    List.of("run-dry-json", "run-spec-dry-json"),
                    "run <prompt> --dry-run --json",
                    "run --spec <file> --dry-run --json"),
            readiness(
                    WayangReadinessContract.READINESS_REPORT,
                    "Shared readiness envelope for one adapter, runtime, persistence, or protocol probe.",
                    List.of()),
            readiness(
                    WayangReadinessContract.READINESS_AGGREGATE,
                    "Shared aggregate readiness envelope combining multiple component readiness reports.",
                    List.of("status-readiness-json", "status-readiness-profile-json"),
                    "status --readiness --json",
                    "status --readiness-profile <profile-id> --json"),
            contractCoverage(
                    WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE,
                    "Command coverage report for SDK-owned JSON contracts.",
                    List.of("contracts-coverage-json"),
                    "contracts --coverage --json"),
            platform(
                    WayangPlatformContract.PLATFORM_STATUS,
                    "Compact platform boundary and adapter status snapshot.",
                    List.of("status-json"),
                    "status --json"),
            platform(
                    WayangPlatformContract.PRODUCT_CATALOG,
                    "Product surface, policy, and reusable profile catalog.",
                    List.of("products-json"),
                    "products --json"),
            platform(
                    WayangPlatformContract.PROFILE_LIST,
                    "Reusable product profile list with optional surface filter.",
                    List.of("profiles-json", "profiles-surface-json"),
                    "profiles --json",
                    "profiles --surface <surface-id> --json"),
            platform(
                    WayangPlatformContract.PROFILE_DETAIL,
                    "One reusable product profile detail envelope.",
                    List.of("profiles-inspect-json"),
                    "profiles inspect <profile-id> --json"),
            platform(
                    WayangPlatformContract.SDK_BOUNDARY_CATALOG,
                    "SDK ownership boundary catalog for package/API separation planning.",
                    List.of("sdk-boundaries-json"),
                    "sdk-boundaries --json"),
            platform(
                    WayangPlatformContract.SDK_BOUNDARY_DETAIL,
                    "One SDK ownership boundary detail envelope.",
                    List.of("sdk-boundaries-inspect-json"),
                    "sdk-boundaries <boundary-id> --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_LIST,
                    "Platform readiness profile catalog with component bindings.",
                    List.of("readiness-profiles-json"),
                    "readiness-profiles --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_DETAIL,
                    "One platform readiness profile detail envelope.",
                    List.of("readiness-profiles-inspect-json"),
                    "readiness-profiles inspect <profile-id> --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_VALIDATION,
                    "Platform readiness profile validation envelope.",
                    List.of("readiness-profiles-check-json"),
                    "readiness-profiles --check --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST,
                    "Platform readiness profile validation policy catalog envelope.",
                    List.of("readiness-profiles-policies-json"),
                    "readiness-profiles policies --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS,
                    "Platform readiness profile registry configuration diagnostics envelope.",
                    List.of("readiness-profiles-config-json"),
                    "readiness-profiles config --json"),
            platform(
                    WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION,
                    "Platform readiness profile registry source resolution envelope.",
                    List.of("readiness-profiles-sources-json"),
                    "readiness-profiles sources --json"),
            standardAlignment(
                    WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH,
                    "Standard-alignment health envelope with readiness, drift, policy, and provider diagnostics.",
                    List.of("standards-health-json"),
                    "standards --json"),
            standardCatalog(
                    WayangStandardCatalogContract.STANDARDS_CATALOG,
                    "Known standards catalog with pinned ids, versions, bindings, aliases, and spec URLs.",
                    List.of("standards-catalog-json"),
                    "standards --catalog --json"),
            skill(
                    WayangSkillContract.SKILL_DISCOVERY,
                    "Dynamic skill discovery envelope with query, facets, skill ids, and capability entries.",
                    List.of(
                            "skills-list-json",
                            "skills-list-profile-json",
                            "skills-search-json",
                            "skills-search-profile-json"),
                    "skills list --surface assistant-agent --json",
                    "skills list --profile <profile-id> --json",
                    "skills search rag --surface assistant-agent --json",
                    "skills search gamelan --profile <profile-id> --json"),
            skill(
                    WayangSkillContract.SKILL_DETAIL,
                    "One stable dynamic skill capability detail envelope.",
                    List.of("skills-inspect-json"),
                    "skills inspect <skill-id> --json"),
            providerCapability(
                    WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY,
                    "Cross-module provider capability discovery envelope with query, facets, and capability entries.",
                    List.of(
                            "providers-json",
                            "providers-list-json",
                            "providers-search-json"),
                    "providers --json",
                    "providers list --module a2ui --json",
                    "providers search lifecycle --surface coding-agent --json"),
            providerCapability(
                    WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL,
                    "One stable cross-module provider capability detail envelope.",
                    List.of("providers-inspect-json"),
                    "providers inspect <capability-id> --json"),
            commandDiscovery(
                    WayangCommandDiscoveryContract.COMMANDS_DISCOVERY,
                    "Command discovery envelope with query, facets, command ids, contract facets, and optional command entries.",
                    List.of(
                            "commands-surface-json",
                            "commands-profile-json",
                            "commands-index-json",
                            "commands-id-json",
                            "commands-contract-json-schema-id-json"),
                    "commands --surface <surface-id> --json",
                    "commands --profile <profile-id> --json",
                    "commands --index --json",
                    "commands --id <command-id> --json",
                    "commands --contract-json-schema-id <schema-id> --json"),
            workbenchDiscovery(
                    WayangWorkbenchContract.WORKBENCH_DISCOVERY,
                    "Workbench discovery envelope with platform status, product catalog, command query, commands, and next actions.",
                    List.of(
                            "workbench-surface-json",
                            "workbench-profile-json",
                            "workbench-command-json",
                            "workbench-contract-json-schema-id-json"),
                    "workbench --surface <surface-id> --json",
                    "workbench --profile <profile-id> --json",
                    "workbench --surface assistant-agent --category Runs --id run-session-context --json",
                    "workbench --contract-json-schema-id <schema-id> --json"));

    private static final WayangContractIndex INDEX = WayangContractIndex.of(CONTRACTS);

    private WayangContractCatalog() {
    }

    public static List<WayangContractDescriptor> defaultContracts() {
        return INDEX.contracts();
    }

    public static WayangContractDiscovery discover(WayangContractQuery query) {
        WayangContractQuery normalized = query == null ? WayangContractQuery.all() : query;
        return WayangContractDiscovery.of(
                normalized,
                INDEX.contractsForQuery(normalized),
                INDEX.contracts().size());
    }

    private static WayangContractDescriptor lifecycle(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.lifecycle(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor planning(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.planning(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor commandDiscovery(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.commandDiscovery(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor platform(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.platform(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor readiness(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.readiness(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor contractCoverage(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.contractCoverage(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor standardCatalog(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.standardCatalog(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor standardAlignment(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.standardAlignment(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor skill(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.skill(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor providerCapability(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.providerCapability(envelope, description, commandIds, commands);
    }

    private static WayangContractDescriptor workbenchDiscovery(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return WayangContractDescriptors.workbenchDiscovery(envelope, description, commandIds, commands);
    }
}
