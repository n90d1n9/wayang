package tech.kayys.wayang.agenticcommerce.wayang;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeProbeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime-neutral bundle for Agentic Commerce Wayang checkout integration.
 */
public final class AgenticCommerceWayangRuntime {

    private final AgenticCommerceConnector connector;
    private final AgenticCommerceConnectorFactoryConfig connectorFactoryConfig;
    private final AgenticCommerceConnectorConfig connectorConfig;
    private final AgenticCommerceHttpAdapterConfig httpConfig;
    private final AgenticCommerceConnectorPolicy connectorPolicy;
    private final AgenticCommerceCheckoutService checkoutService;
    private final AgenticCommerceHttpAdapter httpAdapter;
    private final AgenticCommerceCheckoutSkillDispatcher skillDispatcher;

    public AgenticCommerceWayangRuntime(AgenticCommerceConnector connector) {
        this(connector, AgenticCommerceWayangRuntimeConfig.defaults());
    }

    public AgenticCommerceWayangRuntime(
            AgenticCommerceConnector connector,
            AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        this(
                connector,
                runtimeConfig == null
                        ? AgenticCommerceConnectorFactoryConfig.defaults()
                        : runtimeConfig.connectorFactoryConfig(),
                runtimeConfig == null ? AgenticCommerceConnectorConfig.defaults() : runtimeConfig.connectorConfig(),
                runtimeConfig == null ? AgenticCommerceHttpAdapterConfig.defaults() : runtimeConfig.httpConfig(),
                runtimeConfig == null ? AgenticCommerceConnectorPolicy.defaults() : runtimeConfig.connectorPolicy());
    }

    public AgenticCommerceWayangRuntime(
            AgenticCommerceConnector connector,
            AgenticCommerceConnectorConfig connectorConfig,
            AgenticCommerceHttpAdapterConfig httpConfig) {
        this(
                connector,
                AgenticCommerceConnectorFactoryConfig.defaults(),
                connectorConfig,
                httpConfig,
                AgenticCommerceConnectorPolicy.defaults());
    }

    public AgenticCommerceWayangRuntime(
            AgenticCommerceConnector connector,
            AgenticCommerceConnectorFactoryConfig connectorFactoryConfig,
            AgenticCommerceConnectorConfig connectorConfig,
            AgenticCommerceHttpAdapterConfig httpConfig,
            AgenticCommerceConnectorPolicy connectorPolicy) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.connectorFactoryConfig = connectorFactoryConfig == null
                ? AgenticCommerceConnectorFactoryConfig.defaults()
                : connectorFactoryConfig;
        this.connectorConfig = connectorConfig == null ? AgenticCommerceConnectorConfig.defaults() : connectorConfig;
        this.httpConfig = httpConfig == null ? AgenticCommerceHttpAdapterConfig.defaults() : httpConfig;
        this.connectorPolicy = connectorPolicy == null ? AgenticCommerceConnectorPolicy.defaults() : connectorPolicy;
        this.checkoutService = new AgenticCommerceCheckoutService(this.connector, this.connectorConfig);
        this.httpAdapter = new AgenticCommerceHttpAdapter(this.connector, this.httpConfig);
        this.skillDispatcher = AgenticCommerceCheckoutSkillDispatcher.of(this.checkoutService);
    }

    public static AgenticCommerceWayangRuntime inMemory() {
        return new AgenticCommerceWayangRuntime(new InMemoryAgenticCommerceConnector());
    }

    public static AgenticCommerceWayangRuntime inMemory(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        return new AgenticCommerceWayangRuntime(new InMemoryAgenticCommerceConnector(), runtimeConfig);
    }

    public static AgenticCommerceWayangRuntime of(AgenticCommerceConnector connector) {
        return new AgenticCommerceWayangRuntime(connector);
    }

    public static AgenticCommerceWayangRuntime configured(
            AgenticCommerceConnector connector,
            AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        return new AgenticCommerceWayangRuntime(connector, runtimeConfig);
    }

    public static AgenticCommerceWayangRuntime configured(
            AgenticCommerceConnector connector,
            AgenticCommerceConnectorConfig connectorConfig,
            AgenticCommerceHttpAdapterConfig httpConfig) {
        return new AgenticCommerceWayangRuntime(connector, connectorConfig, httpConfig);
    }

    public AgenticCommerceConnector connector() {
        return connector;
    }

    public AgenticCommerceConnectorFactoryConfig connectorFactoryConfig() {
        return connectorFactoryConfig;
    }

    public AgenticCommerceConnectorConfig connectorConfig() {
        return connectorConfig;
    }

    public AgenticCommerceHttpAdapterConfig httpConfig() {
        return httpConfig;
    }

    public AgenticCommerceConnectorPolicy connectorPolicy() {
        return connectorPolicy;
    }

    public AgenticCommerceCheckoutService checkoutService() {
        return checkoutService;
    }

    public AgenticCommerceHttpAdapter httpAdapter() {
        return httpAdapter;
    }

    public AgenticCommerceCheckoutSkillDispatcher skillDispatcher() {
        return skillDispatcher;
    }

    public AgenticCommerceWayangRuntimeConfig runtimeConfig() {
        return new AgenticCommerceWayangRuntimeConfig(
                connectorFactoryConfig,
                connectorConfig,
                httpConfig,
                connectorPolicy);
    }

    public AgenticCommerceWayangManifest manifest() {
        return AgenticCommerceWayangManifest.configured(
                runtimeConfig(),
                AgenticCommerceWayangBootstrapConfig.defaults());
    }

    public AgenticCommerceWayangManifest manifest(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        return AgenticCommerceWayangManifest.configured(runtimeConfig(), bootstrapConfig);
    }

    public AgenticCommerceHttpResponse dispatch(AgenticCommerceHttpRequest request) {
        return httpAdapter.dispatch(request);
    }

    public AgenticCommerceCheckoutHttpSmokeResult smoke() {
        return checkoutService.smoke();
    }

    public AgenticCommerceCheckoutHttpSmokeProbeResult smokeProbe() {
        return httpAdapter.smokeProbe();
    }

    public AgenticCommerceConnectorContractReport connectorContract() {
        return AgenticCommerceConnectorContractHarness.checkoutLifecycle().run(this);
    }

    public AgenticCommerceConnectorDiagnostics connectorDiagnostics() {
        return AgenticCommerceConnectorDiagnostics.from(this);
    }

    public AgenticCommerceConnectorDiagnostics connectorDiagnostics(
            AgenticCommerceConnectorContractReport contractReport) {
        return AgenticCommerceConnectorDiagnostics.from(this, contractReport);
    }

    public AgenticCommerceHttpBindingReport bindingReport() {
        return httpAdapter.bindingReport();
    }

    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        return skillDispatcher.execute(context);
    }

    public Uni<Map<String, Object>> executeBySkillId(String skillId, Map<String, Object> context) {
        return skillDispatcher.executeBySkillId(skillId, context);
    }

    public Uni<Map<String, Object>> executeByOperation(String operation, Map<String, Object> context) {
        return skillDispatcher.executeByOperation(operation, context);
    }

    public AgenticCommerceSkillRegistration installSkills(SkillRegistry registry) {
        return AgenticCommerceCheckoutSkillRegistryInstaller.installAll(registry, checkoutService);
    }

    public AgenticCommerceSkillRegistration installDefinitions(SkillRegistry registry) {
        return AgenticCommerceCheckoutSkillRegistryInstaller.installDefinitions(registry);
    }

    public AgenticCommerceSkillRegistration installRuntimeSkills(SkillRegistry registry) {
        return AgenticCommerceCheckoutSkillRegistryInstaller.installRuntimeSkills(registry, checkoutService);
    }

    public AgenticCommerceWayangBootstrapReport bootstrap(SkillRegistry registry) {
        return bootstrap(registry, AgenticCommerceWayangBootstrapConfig.defaults());
    }

    public AgenticCommerceWayangBootstrapReport bootstrapDefinitions(SkillRegistry registry) {
        return bootstrap(registry, AgenticCommerceWayangBootstrapConfig.definitionsOnly());
    }

    public AgenticCommerceWayangBootstrapReport bootstrapRuntimeSkills(SkillRegistry registry) {
        return bootstrap(registry, AgenticCommerceWayangBootstrapConfig.runtimeSkillsOnly());
    }

    public AgenticCommerceWayangBootstrapReport bootstrap(
            SkillRegistry registry,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangBootstrapConfig config = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        AgenticCommerceSkillRegistration registration = AgenticCommerceCheckoutSkillRegistryInstaller.install(
                registry,
                checkoutService,
                config.skillIds(),
                config.includeDefinitions(),
                config.includeRuntimeSkills());
        return AgenticCommerceWayangBootstrapReport.from(this, registration, config);
    }

    public AgenticCommerceWayangBootstrapReport bootstrap(
            SkillRegistry registry,
            List<String> skillIds,
            boolean includeDefinitions,
            boolean includeRuntimeSkills) {
        return bootstrap(registry, AgenticCommerceWayangBootstrapConfig.builder()
                .skillIds(skillIds)
                .includeDefinitions(includeDefinitions)
                .includeRuntimeSkills(includeRuntimeSkills)
                .build());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("connector", connector.getClass().getName());
        values.put("connectorConfig", connectorConfig.toMap());
        values.put("httpConfig", httpConfig.toMap());
        values.put("runtimeConfig", runtimeConfig().toMap());
        values.put("checkoutSkillDispatcher", skillDispatcher.toMap());
        values.put("bindingReport", bindingReport().toMap());
        values.put("connectorDiagnostics", connectorDiagnostics().toMap());
        return Map.copyOf(values);
    }
}
