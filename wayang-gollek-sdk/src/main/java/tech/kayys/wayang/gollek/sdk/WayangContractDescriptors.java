package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public final class WayangContractDescriptors {

    public static final String DOMAIN_CONTRACTS = "contracts";
    public static final String DOMAIN_LIFECYCLE = "lifecycle";
    public static final String DOMAIN_PLANNING = "planning";
    public static final String DOMAIN_PLATFORM = "platform";
    public static final String DOMAIN_READINESS = "readiness";
    public static final String DOMAIN_STANDARDS = "standards";
    public static final String DOMAIN_SKILLS = "skills";
    public static final String DOMAIN_PROVIDERS = "providers";
    public static final String DOMAIN_WORKBENCH = "workbench";

    private static final WayangContractDescriptor EMPTY =
            new WayangContractDescriptor("", 1, "", "", "", List.of(), List.of());

    private WayangContractDescriptors() {
    }

    public static WayangContractDescriptor empty() {
        return EMPTY;
    }

    public static WayangContractDescriptor of(
            String schema,
            int version,
            String envelope,
            String domain,
            String description,
            List<String> commandIds,
            List<String> commands) {
        return new WayangContractDescriptor(schema, version, envelope, domain, description, commandIds, commands);
    }

    public static WayangContractDescriptor lifecycle(String envelope) {
        return lifecycle(envelope, "", List.of());
    }

    public static WayangContractDescriptor lifecycle(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                AgentRunLifecycleContract.SCHEMA,
                AgentRunLifecycleContract.VERSION,
                envelope,
                DOMAIN_LIFECYCLE,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor planning(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                envelope,
                DOMAIN_PLANNING,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor commandDiscovery(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangCommandDiscoveryContract.SCHEMA,
                WayangCommandDiscoveryContract.VERSION,
                envelope,
                DOMAIN_WORKBENCH,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor platform(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangPlatformContract.SCHEMA,
                WayangPlatformContract.VERSION,
                envelope,
                DOMAIN_PLATFORM,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor readiness(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangReadinessContract.SCHEMA,
                WayangReadinessContract.VERSION,
                envelope,
                DOMAIN_READINESS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor contractCoverage(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangContractCoverageContract.SCHEMA,
                WayangContractCoverageContract.VERSION,
                envelope,
                DOMAIN_CONTRACTS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor standardCatalog(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangStandardCatalogContract.SCHEMA,
                WayangStandardCatalogContract.VERSION,
                envelope,
                DOMAIN_STANDARDS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor standardAlignment(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangStandardAlignmentContract.SCHEMA,
                WayangStandardAlignmentContract.VERSION,
                envelope,
                DOMAIN_STANDARDS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor skill(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangSkillContract.SCHEMA,
                WayangSkillContract.VERSION,
                envelope,
                DOMAIN_SKILLS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor providerCapability(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangProviderCapabilityContract.SCHEMA,
                WayangProviderCapabilityContract.VERSION,
                envelope,
                DOMAIN_PROVIDERS,
                description,
                commandIds,
                List.of(commands));
    }

    public static WayangContractDescriptor workbenchDiscovery(
            String envelope,
            String description,
            List<String> commandIds,
            String... commands) {
        return of(
                WayangWorkbenchContract.SCHEMA,
                WayangWorkbenchContract.VERSION,
                envelope,
                DOMAIN_WORKBENCH,
                description,
                commandIds,
                List.of(commands));
    }

    public static String jsonSchemaId(String schema, int version, String envelope) {
        return WayangContractKey.of(schema, version, envelope).jsonSchemaId();
    }
}
