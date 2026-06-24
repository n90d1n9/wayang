package tech.kayys.wayang.gollek.sdk;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface WayangGollekSdk extends AutoCloseable {

    WayangPlatformStatus status();

    List<ProductSurface> productSurfaces();

    default List<ProductSurfacePolicy> productSurfacePolicies() {
        return WayangProductCatalog.defaultPolicies();
    }

    default ProductSurfacePolicy productSurfacePolicy(String surfaceId) {
        return WayangProductCatalog.policyFor(surfaceId);
    }

    default List<ProductProfile> productProfiles() {
        return WayangProductCatalog.defaultProfiles();
    }

    default List<ProductProfile> productProfilesForSurface(String surfaceId) {
        return WayangProductCatalog.profilesForSurface(surfaceId);
    }

    default List<WayangContractDescriptor> contracts() {
        return contractDiscovery(WayangContractQuery.all()).contracts();
    }

    default WayangContractDiscovery contractDiscovery(WayangContractQuery query) {
        return WayangContractCatalog.discover(query);
    }

    default WayangContractDiscovery contractDiscovery(WayangContractKey key) {
        return contractDiscovery(WayangContractQuery.forKey(key));
    }

    default WayangContractJsonSchema contractJsonSchema(WayangContractDescriptor contract) {
        return WayangContractJsonSchemas.schema(contract);
    }

    default WayangContractJsonSchemaBundle contractJsonSchemaBundle(WayangContractDiscovery discovery) {
        return WayangContractJsonSchemas.bundle(discovery);
    }

    default WayangContractJsonSchemaBundle contractJsonSchemaBundle(WayangContractQuery query) {
        return contractJsonSchemaBundle(contractDiscovery(query));
    }

    default WayangContractJsonSchemaBundle contractJsonSchemaBundle(WayangContractKey key) {
        return contractJsonSchemaBundle(contractDiscovery(key));
    }

    default WayangContractIntegrityReport contractIntegrity() {
        return WayangContractIntegrity.validateDefault();
    }

    default WayangContractCommandCoverageReport contractCommandCoverage() {
        return WayangContractCommandCoverage.defaultCoverage();
    }

    default WayangStandardCatalog standardsCatalog() {
        return WayangStandardCatalog.defaultCatalog();
    }

    default WayangStandardAlignmentPortfolio standardAlignmentPortfolio() {
        return standardAlignmentProviderDiscovery().portfolio();
    }

    default WayangStandardAlignmentProviderDiscovery standardAlignmentProviderDiscovery() {
        return WayangStandardAlignmentProviders.discover();
    }

    default WayangStandardAlignmentHealthReport standardAlignmentHealth() {
        return standardAlignmentHealth(WayangStandardAlignmentPolicyConfig.none());
    }

    default WayangStandardAlignmentHealthReport standardAlignmentHealth(WayangStandardAlignmentPolicyConfig config) {
        WayangStandardAlignmentProviderDiscovery discovery = standardAlignmentProviderDiscovery();
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                discovery.portfolio(),
                config,
                discovery.providerDiagnostics());
    }

    default ProductProfile productProfile(String profileId) {
        return WayangProductCatalog.profileFor(profileId);
    }

    default List<WayangSdkBoundary> sdkBoundaries() {
        return WayangSdkBoundaryCatalog.defaultBoundaries();
    }

    default WayangSdkBoundary sdkBoundary(String boundaryId) {
        return WayangSdkBoundaryCatalog.require(boundaryId);
    }

    default WayangSdkBoundaryCatalogValidationReport sdkBoundaryCatalogValidation() {
        return WayangSdkBoundaryCatalog.validateDefault();
    }

    default WayangSdkBoundaryCatalogValidationReport validateSdkBoundaries(
            List<WayangSdkBoundary> boundaries) {
        return WayangSdkBoundaryCatalog.validate(boundaries);
    }

    default WayangCodeAgentExtensionDiscovery codeAgentExtensions(
            WayangCodeAgentContext context) {
        return WayangCodeAgentExtensions.discover(context);
    }

    default WayangCodeAgentExtensionDiscovery codeAgentExtensions() {
        return codeAgentExtensions(WayangCodeAgentContext.builder().build());
    }

    default SurfacePolicyAssessment assessRunPolicy(AgentRunRequest request) {
        return SurfacePolicyPreflight.assess(request);
    }

    default AgentRunReadiness assessRunReadiness(AgentRunRequest request) {
        return AgentRunReadiness.assess(skillRegistry(), request);
    }

    default WayangReadinessReport storageReadiness() {
        return WayangStorageReadiness.assess(WayangStorageConfig.memory());
    }

    default WayangReadinessReport platformReadiness() {
        return WayangPlatformReadiness.assess(this);
    }

    default WayangReadinessReport platformReadiness(String profileId) {
        return WayangPlatformReadiness.assess(this, profileId);
    }

    default WayangReadinessReport platformReadiness(WayangPlatformReadinessProfile profile) {
        return WayangPlatformReadiness.assess(this, profile);
    }

    default List<WayangPlatformReadinessProfileDescriptor> platformReadinessProfiles() {
        return WayangPlatformReadinessProfileCatalog.defaultProfiles();
    }

    default List<WayangPlatformReadinessProfileDescriptor> platformReadinessProfiles(
            WayangPlatformReadinessProfileRegistry registry) {
        return platformReadinessProfileRegistryResolution(registry).profiles();
    }

    default WayangPlatformReadinessProfileDescriptor platformReadinessProfile(String profileId) {
        return WayangPlatformReadinessProfileCatalog.profile(profileId);
    }

    default WayangPlatformReadinessProfileRegistry platformReadinessProfileRegistry() {
        return WayangPlatformReadinessProfileRegistry.defaultRegistry();
    }

    default WayangPlatformReadinessProfileRegistryResolution platformReadinessProfileRegistryResolution() {
        return platformReadinessProfileRegistry().resolve();
    }

    default WayangPlatformReadinessProfileRegistryConfigDiagnostics
            platformReadinessProfileRegistryConfigDiagnostics() {
        return WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics();
    }

    default WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport
            platformReadinessProfileExternalReaderProviderDiscovery() {
        return WayangPlatformReadinessProfileExternalReaderProviders.discoveryReport(WayangGollekSdkConfig.local());
    }

    default WayangPlatformReadinessProfileRegistryPreflightReport
            platformReadinessProfileRegistryPreflight() {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                platformReadinessProfileRegistryConfigDiagnostics();
        return WayangPlatformReadinessProfileRegistryPreflightReport.of(
                diagnostics,
                platformReadinessProfileExternalReaderProviderDiscovery(),
                platformReadinessProfileRegistryResolution());
    }

    default WayangPlatformReadinessProfileRegistryResolution platformReadinessProfileRegistryResolution(
            WayangPlatformReadinessProfileRegistry registry) {
        WayangPlatformReadinessProfileRegistry resolved = registry == null
                ? platformReadinessProfileRegistry()
                : registry;
        return resolved.resolve();
    }

    default WayangPlatformReadinessProfileRegistryResolution platformReadinessProfileRegistryResolution(
            WayangPlatformReadinessProfileSource source) {
        return WayangPlatformReadinessProfileRegistry.withBuiltInFallback(source).resolve();
    }

    default WayangPlatformReadinessProfileRegistryResolution platformReadinessProfileRegistryResolution(
            WayangPlatformReadinessProfileSource source,
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return WayangPlatformReadinessProfileRegistry.of(
                        source,
                        WayangPlatformReadinessProfileBuiltInSource.create(),
                        policy)
                .resolve();
    }

    default WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation() {
        return WayangPlatformReadinessProfileValidation.validateDefault();
    }

    default WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation(String policyId) {
        return platformReadinessProfileValidation(
                WayangPlatformReadinessProfileValidationPolicies.policy(policyId));
    }

    default WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation(
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return validatePlatformReadinessProfiles(platformReadinessProfiles(), policy);
    }

    default List<String> platformReadinessProfileValidationPolicyIds() {
        return WayangPlatformReadinessProfileValidationPolicies.policyIds();
    }

    default List<WayangPlatformReadinessProfileValidationPolicyDescriptor>
            platformReadinessProfileValidationPolicies() {
        return WayangPlatformReadinessProfileValidationPolicies.descriptors();
    }

    default WayangPlatformReadinessProfileValidationPolicyDescriptor platformReadinessProfileValidationPolicy(
            String policyId) {
        return WayangPlatformReadinessProfileValidationPolicies.descriptor(policyId);
    }

    default WayangPlatformReadinessProfileValidationReport validatePlatformReadinessProfiles(
            List<WayangPlatformReadinessProfileDescriptor> profiles) {
        return WayangPlatformReadinessProfileValidation.validate(profiles);
    }

    default WayangPlatformReadinessProfileValidationReport validatePlatformReadinessProfiles(
            List<WayangPlatformReadinessProfileDescriptor> profiles,
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return WayangPlatformReadinessProfileValidation.validate(profiles, policy);
    }

    WayangWorkbenchModel workbench();

    default WayangWorkbenchModel workbench(WorkbenchCommandQuery query) {
        return WayangCommandDiscoveryService.create().filterWorkbench(workbench(), query);
    }

    default WayangWorkbenchModel workbench(String surfaceId, String category, String commandId) {
        return workbench(WorkbenchCommandQuery.of(surfaceId, category, commandId));
    }

    default WayangWorkbenchModel workbenchForProfile(String profileId, String category, String commandId) {
        return workbench(WorkbenchCommandQuery.forProfile(profileId, category, commandId));
    }

    default WayangWorkbenchModel workbenchForContractJsonSchemaId(String contractJsonSchemaId) {
        return workbench(WorkbenchCommandQuery.forContractJsonSchemaId(contractJsonSchemaId));
    }

    default WayangWorkbenchModel workbenchForContractKey(WayangContractKey key) {
        return workbench(WorkbenchCommandQuery.forContractKey(key));
    }

    default WorkbenchCommandDiscovery commandDiscovery(WorkbenchCommandQuery query) {
        return WayangCommandDiscoveryService.create().commandDiscovery(workbench(), query);
    }

    default WorkbenchCommandDiscovery commandDiscovery(String surfaceId, String category, String commandId) {
        return commandDiscovery(WorkbenchCommandQuery.of(surfaceId, category, commandId));
    }

    default WorkbenchCommandDiscovery commandDiscoveryForProfile(String profileId, String category, String commandId) {
        return commandDiscovery(WorkbenchCommandQuery.forProfile(profileId, category, commandId));
    }

    default WorkbenchCommandDiscovery commandDiscoveryForContractJsonSchemaId(String contractJsonSchemaId) {
        return commandDiscovery(WorkbenchCommandQuery.forContractJsonSchemaId(contractJsonSchemaId));
    }

    default WorkbenchCommandDiscovery commandDiscoveryForContractKey(WayangContractKey key) {
        return commandDiscovery(WorkbenchCommandQuery.forContractKey(key));
    }

    default List<WorkbenchCommand> discoverCommands(WorkbenchCommandQuery query) {
        return commandDiscovery(query).commands();
    }

    default List<WorkbenchCommand> discoverCommands(String surfaceId, String category, String commandId) {
        return discoverCommands(WorkbenchCommandQuery.of(surfaceId, category, commandId));
    }

    default List<WorkbenchCommand> discoverCommandsForProfile(String profileId, String category, String commandId) {
        return discoverCommands(WorkbenchCommandQuery.forProfile(profileId, category, commandId));
    }

    default List<WorkbenchCommand> discoverCommandsForContractJsonSchemaId(String contractJsonSchemaId) {
        return discoverCommands(WorkbenchCommandQuery.forContractJsonSchemaId(contractJsonSchemaId));
    }

    default List<WorkbenchCommand> discoverCommandsForContractKey(WayangContractKey key) {
        return discoverCommands(WorkbenchCommandQuery.forContractKey(key));
    }

    default SkillRegistry skillRegistry() {
        return WayangSkillCatalog.defaultRegistry();
    }

    default List<RegisteredSkill> skills(AgentSkillQuery query) {
        return skillDiscovery(query).skills();
    }

    default List<RegisteredSkill> skills() {
        return skills(AgentSkillQuery.all());
    }

    default List<RegisteredSkill> skillsForProfile(String profileId) {
        return skills(AgentSkillQuery.forProfile(profileId, null, null));
    }

    default RegisteredSkill skill(String skillId) {
        return skillRegistry().require(skillId);
    }

    default WayangProviderCapabilityRegistry providerCapabilityRegistry() {
        return WayangProviderCapabilityCatalog.defaultRegistry();
    }

    default List<WayangProviderCapabilityDescriptor> providerCapabilities() {
        return providerCapabilities(WayangProviderCapabilityQuery.all());
    }

    default List<WayangProviderCapabilityDescriptor> providerCapabilities(WayangProviderCapabilityQuery query) {
        return providerCapabilityDiscovery(query).capabilities();
    }

    /** Dynamically register provider metadata from a provider JAR file. Implementations may probe the JAR to discover provider classes and register capabilities. */
    default void loadProviderJar(String jarPath) {
        throw new UnsupportedOperationException("loadProviderJar is not supported by this WayangGollekSdk implementation");
    }

    /**
     * Optional YAFF transport provider (shared-memory) discovery.
     */
    default java.util.Optional<WayangYaffTransportProvider> yaffTransportProvider() {
        return java.util.Optional.empty();
    }

    default WayangProviderCapabilityDescriptor providerCapability(String capabilityId) {
        return providerCapabilityRegistry().require(capabilityId);
    }

    default WayangProviderCapabilityDiscovery providerCapabilityDiscovery(WayangProviderCapabilityQuery query) {
        return providerCapabilityDiscovery(query, "");
    }

    default WayangProviderCapabilityDiscovery providerCapabilityDiscovery(
            WayangProviderCapabilityQuery query,
            String search) {
        return WayangProviderCapabilityDiscoveryService.create()
                .discover(providerCapabilityRegistry(), query, search);
    }

    default WayangProviderCapabilityDiscovery providerCapabilityDiscovery(String search) {
        return providerCapabilityDiscovery(WayangProviderCapabilityQuery.all(), search);
    }

    default AgentSkillDiscovery skillDiscovery(AgentSkillQuery query) {
        return skillDiscovery(query, "");
    }

    default AgentSkillDiscovery skillDiscovery(AgentSkillQuery query, String search) {
        return AgentSkillDiscoveryService.create().discover(skillRegistry(), query, search);
    }

    default AgentSkillDiscovery skillDiscoveryForProfile(String profileId, String search) {
        return skillDiscovery(AgentSkillQuery.forProfile(profileId, null, null), search);
    }

    WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request);

    /** Provider control: allow CLI and tooling to prefer a specific provider id. */
    void setPreferredProvider(String providerId);

    Optional<String> getPreferredProvider();

    /** List known provider ids from capability registry. */
    List<String> listAvailableProviders();

    HarnessPlan planHarness(HarnessPlanRequest request);

    default AgentRunPreview previewRun(AgentRunRequest request) {
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        return AgentRunPreview.from(
                normalized,
                toCoreAgentRequest(normalized),
                assessRunPolicy(normalized),
                AgentRunSkillPreflight.assess(skillRegistry(), normalized));
    }

    AgentRunResult run(AgentRunRequest request);

    default AgentRunStatus runStatus(String runId) {
        return AgentRunStatus.unknown(
                runId,
                "Run status storage is not configured for this Wayang SDK.");
    }

    default AgentRunHistory runHistory() {
        return runHistory(AgentRunHistoryQuery.all());
    }

    default AgentRunHistory runHistory(AgentRunHistoryQuery query) {
        return new AgentRunHistory(
                query,
                List.of(),
                0,
                "Run status storage is not configured for this Wayang SDK.");
    }

    default AgentRunStoreDiagnostics runStoreDiagnostics() {
        return AgentRunStore.memory().diagnostics();
    }

    default AgentRunStoreVerification runStoreVerification() {
        return AgentRunStore.memory().verification();
    }

    default AgentRunStoreCompactionPreview runStoreCompactionPreview() {
        return AgentRunStore.memory().compactionPreview();
    }

    default AgentRunStoreCompactionResult compactRunStore() {
        return AgentRunStore.memory().compact();
    }

    default AgentRunEvents runEvents(String runId) {
        return runEvents(runId, AgentRunEventsQuery.all());
    }

    default AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        return new AgentRunEvents(
                runId,
                query == null ? AgentRunEventsQuery.all() : query,
                List.of(),
                0,
                "Run event storage is not configured for this Wayang SDK.");
    }

    default AgentRunEventsFollowResult followRunEvents(String runId, AgentRunEventsFollowOptions options) {
        return followRunEvents(runId, options, null);
    }

    default AgentRunEventsFollowResult followRunEvents(
            String runId,
            AgentRunEventsFollowOptions options,
            Consumer<AgentRunEvents> eventConsumer) {
        return AgentRunLifecycleService.followEvents(runId, options, this::runEvents, eventConsumer);
    }

    default AgentRunInspection inspectRun(String runId) {
        return inspectRun(runId, AgentRunEventsQuery.all());
    }

    default AgentRunInspection inspectRun(String runId, AgentRunEventsQuery query) {
        return AgentRunLifecycleService.inspection(runId, runStatus(runId), runEvents(runId, query));
    }

    default AgentRunForgetResult forgetRun(String runId) {
        return AgentRunForgetResult.notFound(
                runId,
                "Run status storage is not configured for this Wayang SDK.");
    }

    default AgentRunCancelResult cancelRun(String runId, String reason) {
        return AgentRunCancelResult.notCancellable(
                runId,
                "Run cancellation is not configured for this Wayang SDK.");
    }

    default AgentRunWaitResult waitForRun(String runId, AgentRunWaitOptions options) {
        return AgentRunLifecycleService.waitForRun(runId, options, this::runStatus);
    }

    default AgentRequest toCoreAgentRequest(AgentRunRequest request) {
        return new WayangAgentRequestMapper().toAgentRequest(request);
    }

    @Override
    default void close() {
    }

    static WayangGollekSdk local() {
        return WayangGollekSdkFactory.createLocalSdk();
    }

    static WayangGollekSdk remote(String endpoint, String apiKey) {
        return WayangGollekSdkFactory.createRemoteSdk(endpoint, apiKey);
    }

}
