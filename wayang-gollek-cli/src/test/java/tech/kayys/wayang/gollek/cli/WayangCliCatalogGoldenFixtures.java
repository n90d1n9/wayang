package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunLifecycleContract;
import tech.kayys.wayang.gollek.sdk.WayangCommandDiscoveryContract;
import tech.kayys.wayang.gollek.sdk.WayangContractCatalog;
import tech.kayys.wayang.gollek.sdk.WayangContractCoverageContract;
import tech.kayys.wayang.gollek.sdk.WayangContractDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangContractDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangContractQuery;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangPlatformContract;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityContract;
import tech.kayys.wayang.gollek.sdk.WayangReadinessContract;
import tech.kayys.wayang.gollek.sdk.WayangSkillContract;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentContract;
import tech.kayys.wayang.gollek.sdk.WayangStandardCatalogContract;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchContract;

import java.util.List;
import java.util.function.Supplier;

final class WayangCliCatalogGoldenFixtures {
    private static final Supplier<WayangGollekSdk> LOCAL_SDK = WayangGollekSdk::local;
    private static final List<WayangCliGoldenFixtures.GoldenFixture> ALL = List.of(
            localPayload(
                    "status-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.PLATFORM_STATUS),
                    "status",
                    "--json"),
            localPayload(
                    "status-readiness-json.golden",
                    descriptor(WayangReadinessContract.SCHEMA, WayangReadinessContract.READINESS_AGGREGATE),
                    "status",
                    "--readiness",
                    "--json"),
            localPayload(
                    "status-readiness-profile-json.golden",
                    descriptor(WayangReadinessContract.SCHEMA, WayangReadinessContract.READINESS_AGGREGATE),
                    "status",
                    "--readiness-profile",
                    "default",
                    "--json"),
            localPayload(
                    "products-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.PRODUCT_CATALOG),
                    "products",
                    "--json"),
            localPayload(
                    "profiles-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.PROFILE_LIST),
                    "profiles",
                    "--json"),
            localPayload(
                    "profiles-surface-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.PROFILE_LIST),
                    "profiles",
                    "--surface",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "profiles-inspect-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.PROFILE_DETAIL),
                    "profiles",
                    "inspect",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "sdk-boundaries-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.SDK_BOUNDARY_CATALOG),
                    "sdk-boundaries",
                    "--json"),
            localPayload(
                    "sdk-boundaries-inspect-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.SDK_BOUNDARY_DETAIL),
                    "sdk-boundaries",
                    "run",
                    "--json"),
            localPayload(
                    "readiness-profiles-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.READINESS_PROFILE_LIST),
                    "readiness-profiles",
                    "--json"),
            localPayload(
                    "readiness-profiles-inspect-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.READINESS_PROFILE_DETAIL),
                    "readiness-profiles",
                    "inspect",
                    "minimal",
                    "--json"),
            localPayload(
                    "readiness-profiles-check-json.golden",
                    descriptor(WayangPlatformContract.SCHEMA, WayangPlatformContract.READINESS_PROFILE_VALIDATION),
                    "readiness-profiles",
                    "--check",
                    "--json"),
            localPayload(
                    "readiness-profiles-policies-json.golden",
                    descriptor(
                            WayangPlatformContract.SCHEMA,
                            WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST),
                    "readiness-profiles",
                    "policies",
                    "--json"),
            localPayload(
                    "readiness-profiles-config-json.golden",
                    descriptor(
                            WayangPlatformContract.SCHEMA,
                            WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS),
                    "readiness-profiles",
                    "config",
                    "--json"),
            localPayload(
                    "readiness-profiles-sources-json.golden",
                    descriptor(
                            WayangPlatformContract.SCHEMA,
                            WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION),
                    "readiness-profiles",
                    "sources",
                    "--json"),
            localPayload(
                    "commands-index-json.golden",
                    descriptor(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                    "commands",
                    "--surface",
                    "assistant-agent",
                    "--category",
                    "Runs",
                    "--index",
                    "--json"),
            localPayload(
                    "commands-detail-json.golden",
                    descriptor(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                    List.of("commands-id-json"),
                    "commands",
                    "--id",
                    "run-print-spec-output",
                    "--json"),
            localPayload(
                    "commands-surface-json.golden",
                    descriptor(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                    "commands",
                    "--surface",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "commands-profile-json.golden",
                    descriptor(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                    "commands",
                    "--profile",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "commands-contract-json-schema-id-json.golden",
                    descriptor(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                    "commands",
                    "--contract-json-schema-id",
                    jsonSchemaId(AgentRunLifecycleContract.SCHEMA, AgentRunLifecycleContract.RUN_RESULT),
                    "--json"),
            localPayload(
                    "workbench-surface-json.golden",
                    descriptor(WayangWorkbenchContract.SCHEMA, WayangWorkbenchContract.WORKBENCH_DISCOVERY),
                    "workbench",
                    "--surface",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "workbench-profile-json.golden",
                    descriptor(WayangWorkbenchContract.SCHEMA, WayangWorkbenchContract.WORKBENCH_DISCOVERY),
                    "workbench",
                    "--profile",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "workbench-command-json.golden",
                    descriptor(WayangWorkbenchContract.SCHEMA, WayangWorkbenchContract.WORKBENCH_DISCOVERY),
                    "workbench",
                    "--surface",
                    "assistant-agent",
                    "--category",
                    "Runs",
                    "--id",
                    "run-session-context",
                    "--json"),
            localPayload(
                    "workbench-contract-json-schema-id-json.golden",
                    descriptor(WayangWorkbenchContract.SCHEMA, WayangWorkbenchContract.WORKBENCH_DISCOVERY),
                    "workbench",
                    "--contract-json-schema-id",
                    jsonSchemaId(AgentRunLifecycleContract.SCHEMA, AgentRunLifecycleContract.RUN_RESULT),
                    "--json"),
            localPayload(
                    "skills-list-json.golden",
                    descriptor(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DISCOVERY),
                    "skills",
                    "list",
                    "--surface",
                    "assistant-agent",
                    "--source",
                    "rag",
                    "--json"),
            localPayload(
                    "skills-list-profile-json.golden",
                    descriptor(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DISCOVERY),
                    "skills",
                    "list",
                    "--profile",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "skills-inspect-json.golden",
                    descriptor(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DETAIL),
                    "skills",
                    "inspect",
                    "rag",
                    "--json"),
            localPayload(
                    "skills-search-json.golden",
                    descriptor(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DISCOVERY),
                    "skills",
                    "search",
                    "rag",
                    "--surface",
                    "assistant-agent",
                    "--json"),
            localPayload(
                    "skills-search-profile-json.golden",
                    descriptor(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DISCOVERY),
                    "skills",
                    "search",
                    "gamelan",
                    "--profile",
                    "workflow-agent",
                    "--json"),
            localPayload(
                    "providers-json.golden",
                    descriptor(
                            WayangProviderCapabilityContract.SCHEMA,
                            WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY),
                    "providers",
                    "--json"),
            localPayload(
                    "providers-list-json.golden",
                    descriptor(
                            WayangProviderCapabilityContract.SCHEMA,
                            WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY),
                    "providers",
                    "list",
                    "--module",
                    "a2ui",
                    "--json"),
            localPayload(
                    "providers-search-json.golden",
                    descriptor(
                            WayangProviderCapabilityContract.SCHEMA,
                            WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY),
                    "providers",
                    "search",
                    "lifecycle",
                    "--surface",
                    "coding-agent",
                    "--json"),
            localPayload(
                    "providers-inspect-json.golden",
                    descriptor(
                            WayangProviderCapabilityContract.SCHEMA,
                            WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL),
                    "providers",
                    "inspect",
                    "storage.hybrid-persistence",
                    "--json"),
            localUnvalidated("contracts-json.golden", "contracts", "--json"),
            localUnvalidated("contracts-index-json.golden", "contracts", "--domain", "planning", "--index", "--json"),
            localUnvalidated(
                    "contract-run-preview-schema-json.golden",
                    "contracts",
                    "--envelope",
                    "run-preview",
                    "--schema-json"),
            localUnvalidated(
                    "contract-planning-schema-bundle-json.golden",
                    "contracts",
                    "--domain",
                    "planning",
                    "--schema-bundle-json"),
            localUnvalidated(
                    "contract-provider-schema-bundle-json.golden",
                    "contracts",
                    "--domain",
                    "providers",
                    "--schema-bundle-json"),
            localUnvalidated("contracts-check-json.golden", "contracts", "--check", "--json"),
            localPayload(
                    "contracts-coverage-json.golden",
                    descriptor(
                            WayangContractCoverageContract.SCHEMA,
                            WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE),
                    "contracts",
                    "--coverage",
                    "--json"),
            localPayload(
                    "standards-health-json.golden",
                    descriptor(
                            WayangStandardAlignmentContract.SCHEMA,
                            WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH),
                    "standards",
                    "--json"),
            localPayload(
                    "standards-catalog-json.golden",
                    descriptor(WayangStandardCatalogContract.SCHEMA, WayangStandardCatalogContract.STANDARDS_CATALOG),
                    "standards",
                    "--catalog",
                    "--json"));

    private WayangCliCatalogGoldenFixtures() {
    }

    static List<WayangCliGoldenFixtures.GoldenFixture> all() {
        return ALL;
    }

    private static WayangCliGoldenFixtures.GoldenFixture localPayload(
            String name,
            WayangContractDescriptor descriptor,
            String... args) {
        return localPayload(name, descriptor, null, args);
    }

    private static WayangCliGoldenFixtures.GoldenFixture localPayload(
            String name,
            WayangContractDescriptor descriptor,
            List<String> commandIds,
            String... args) {
        if (commandIds == null) {
            return new WayangCliGoldenFixtures.GoldenFixture(
                    name,
                    "local",
                    LOCAL_SDK,
                    List.of(args),
                    0,
                    WayangCliGoldenFixtures.SchemaMode.EXPLICIT,
                    descriptor);
        }
        return new WayangCliGoldenFixtures.GoldenFixture(
                name,
                "local",
                LOCAL_SDK,
                List.of(args),
                commandIds,
                0,
                WayangCliGoldenFixtures.SchemaMode.EXPLICIT,
                descriptor);
    }

    private static WayangCliGoldenFixtures.GoldenFixture localUnvalidated(String name, String... args) {
        return new WayangCliGoldenFixtures.GoldenFixture(
                name,
                "local",
                LOCAL_SDK,
                List.of(args),
                0,
                WayangCliGoldenFixtures.SchemaMode.NONE,
                null);
    }

    private static WayangContractDescriptor descriptor(String schema, String envelope) {
        WayangContractDiscovery discovery = WayangContractCatalog.discover(WayangContractQuery.of(schema, envelope));
        if (discovery.contracts().size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one contract descriptor for " + schema + "/" + envelope
                            + " but found " + discovery.contracts().size());
        }
        return discovery.contracts().get(0);
    }

    private static String jsonSchemaId(String schema, String envelope) {
        return descriptor(schema, envelope).jsonSchemaId();
    }
}
