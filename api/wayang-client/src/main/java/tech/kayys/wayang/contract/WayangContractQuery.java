package tech.kayys.wayang.contract;

import java.util.Optional;

public record WayangContractQuery(
        String schema,
        String envelope,
        String commandId,
        String domain,
        String jsonSchemaId) {

    public WayangContractQuery {
        schema = normalize(schema);
        envelope = normalize(envelope);
        commandId = normalize(commandId);
        domain = normalize(domain);
        jsonSchemaId = normalize(jsonSchemaId);
    }

    public static WayangContractQuery all() {
        return new WayangContractQuery(null, null, null, null, null);
    }

    public static WayangContractQuery forKey(WayangContractKey key) {
        return key == null ? all() : forJsonSchemaId(key.jsonSchemaId());
    }

    public static WayangContractQuery forJsonSchemaId(String jsonSchemaId) {
        return of(null, null, null, null, jsonSchemaId);
    }

    public static WayangContractQuery forCommandId(String commandId) {
        return of(null, null, commandId);
    }

    public static WayangContractQuery forDomain(String domain) {
        return of(null, null, null, domain);
    }

    public static WayangContractQuery lifecycle() {
        return lifecycle(null);
    }

    public static WayangContractQuery lifecycle(String envelope) {
        return of(
                AgentRunLifecycleContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_LIFECYCLE);
    }

    public static WayangContractQuery planning() {
        return planning(null);
    }

    public static WayangContractQuery planning(String envelope) {
        return of(
                AgentRunPlanningContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_PLANNING);
    }

    public static WayangContractQuery platform() {
        return platform(null);
    }

    public static WayangContractQuery platform(String envelope) {
        return of(
                WayangPlatformContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_PLATFORM);
    }

    public static WayangContractQuery readiness() {
        return readiness(null);
    }

    public static WayangContractQuery readiness(String envelope) {
        return of(
                WayangReadinessContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_READINESS);
    }

    public static WayangContractQuery contractCoverage() {
        return contractCoverage(WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE);
    }

    public static WayangContractQuery contractCoverage(String envelope) {
        return of(
                WayangContractCoverageContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_CONTRACTS);
    }

    public static WayangContractQuery standardCatalog() {
        return standardCatalog(WayangStandardCatalogContract.STANDARDS_CATALOG);
    }

    public static WayangContractQuery standardCatalog(String envelope) {
        return of(
                WayangStandardCatalogContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_STANDARDS);
    }

    public static WayangContractQuery standardAlignment() {
        return standardAlignment(WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH);
    }

    public static WayangContractQuery standardAlignment(String envelope) {
        return of(
                WayangStandardAlignmentContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_STANDARDS);
    }

    public static WayangContractQuery skill() {
        return skill(null);
    }

    public static WayangContractQuery skill(String envelope) {
        return of(
                WayangSkillContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_SKILLS);
    }

    public static WayangContractQuery providerCapability() {
        return providerCapability(null);
    }

    public static WayangContractQuery providerCapability(String envelope) {
        return of(
                WayangProviderCapabilityContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_PROVIDERS);
    }

    public static WayangContractQuery commandDiscovery() {
        return commandDiscovery(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY);
    }

    public static WayangContractQuery commandDiscovery(String envelope) {
        return of(
                WayangCommandDiscoveryContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_WORKBENCH);
    }

    public static WayangContractQuery workbenchDiscovery() {
        return workbenchDiscovery(WayangWorkbenchContract.WORKBENCH_DISCOVERY);
    }

    public static WayangContractQuery workbenchDiscovery(String envelope) {
        return of(
                WayangWorkbenchContract.SCHEMA,
                envelope,
                null,
                WayangContractDescriptors.DOMAIN_WORKBENCH);
    }

    public static WayangContractQuery of(String schema, String envelope) {
        return of(schema, envelope, null);
    }

    public static WayangContractQuery of(String schema, String envelope, String commandId) {
        return of(schema, envelope, commandId, null);
    }

    public static WayangContractQuery of(String schema, String envelope, String commandId, String domain) {
        return of(schema, envelope, commandId, domain, null);
    }

    public static WayangContractQuery of(
            String schema,
            String envelope,
            String commandId,
            String domain,
            String jsonSchemaId) {
        return new WayangContractQuery(schema, envelope, commandId, domain, jsonSchemaId);
    }

    public boolean hasSchema() {
        return schema != null;
    }

    public boolean hasEnvelope() {
        return envelope != null;
    }

    public boolean hasCommandId() {
        return commandId != null;
    }

    public boolean hasDomain() {
        return domain != null;
    }

    public boolean hasJsonSchemaId() {
        return jsonSchemaId != null;
    }

    public Optional<WayangContractKey> jsonSchemaKey() {
        return WayangContractKey.parseJsonSchemaId(jsonSchemaId);
    }

    public boolean filtered() {
        return hasSchema() || hasEnvelope() || hasCommandId() || hasDomain() || hasJsonSchemaId();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
