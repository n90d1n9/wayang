package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static discovery manifest for Wayang Agentic Commerce integration.
 */
public record AgenticCommerceWayangManifest(
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceWayangBootstrapConfig bootstrapConfig,
        AgenticCommerceHttpBindingReport bindingReport,
        List<SkillDefinition> checkoutSkills) {

    public AgenticCommerceWayangManifest {
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        bootstrapConfig = bootstrapConfig == null ? AgenticCommerceWayangBootstrapConfig.defaults() : bootstrapConfig;
        bindingReport = bindingReport == null
                ? AgenticCommerceHttpBindingReport.fromConfig(runtimeConfig.httpConfig())
                : bindingReport;
        checkoutSkills = checkoutSkills == null || checkoutSkills.isEmpty()
                ? new AgenticCommerceCheckoutSkillProjector().checkoutSkills()
                : checkoutSkills.stream()
                        .filter(java.util.Objects::nonNull)
                        .toList();
    }

    public static AgenticCommerceWayangManifest defaults() {
        return configured(
                AgenticCommerceWayangRuntimeConfig.defaults(),
                AgenticCommerceWayangBootstrapConfig.defaults());
    }

    public static AgenticCommerceWayangManifest configured(
            AgenticCommerceWayangRuntimeConfig runtimeConfig,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangRuntimeConfig resolvedRuntimeConfig = runtimeConfig == null
                ? AgenticCommerceWayangRuntimeConfig.defaults()
                : runtimeConfig;
        return new AgenticCommerceWayangManifest(
                resolvedRuntimeConfig,
                bootstrapConfig,
                AgenticCommerceHttpBindingReport.fromConfig(resolvedRuntimeConfig.httpConfig()),
                new AgenticCommerceCheckoutSkillProjector().checkoutSkills());
    }

    public int skillCount() {
        return checkoutSkills.size();
    }

    public int routeCount() {
        return bindingReport.routeCount();
    }

    public List<String> skillIds() {
        return checkoutSkills.stream()
                .map(SkillDefinition::id)
                .toList();
    }

    public List<String> operations() {
        return checkoutSkills.stream()
                .map(skill -> AgenticCommerceWayangMaps.text(skill.metadata().get(AgenticCommerceWayang.METADATA_OPERATION)))
                .filter(operation -> !operation.isBlank())
                .distinct()
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("skillCount", skillCount());
        values.put("routeCount", routeCount());
        values.put("operationCount", operations().size());
        values.put("skillIds", skillIds());
        values.put("operations", operations());
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("bindingReport", bindingReport.toMap());
        values.put("checkoutSkills", checkoutSkills.stream().map(this::skill).toList());
        return Map.copyOf(values);
    }

    public String toJson() {
        return AgenticCommerceJson.write(toMap());
    }

    private Map<String, Object> skill(SkillDefinition skill) {
        Map<String, Object> metadata = skill.metadata();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", skill.id());
        values.put("name", AgenticCommerceWayangMaps.text(skill.name()));
        values.put("description", AgenticCommerceWayangMaps.text(skill.description()));
        values.put("category", AgenticCommerceWayangMaps.text(skill.category()));
        values.put("tools", skill.tools());
        values.put("operation", AgenticCommerceWayangMaps.text(metadata.get(AgenticCommerceWayang.METADATA_OPERATION)));
        values.put("httpMethod", AgenticCommerceWayangMaps.text(metadata.get(AgenticCommerceWayang.METADATA_HTTP_METHOD)));
        values.put("pathTemplate", AgenticCommerceWayangMaps.text(metadata.get(AgenticCommerceWayang.METADATA_PATH_TEMPLATE)));
        values.put("inputSchema", AgenticCommerceWayangMaps.copy(map(metadata.get(SkillMetadataKeys.KEY_INPUT_SCHEMA))));
        values.put("outputFormat", AgenticCommerceWayangMaps.text(metadata.get(SkillMetadataKeys.KEY_OUTPUT_FORMAT)));
        values.put("specVersion", AgenticCommerceWayangMaps.text(metadata.get(AgenticCommerceWayang.METADATA_SPEC_VERSION)));
        return Map.copyOf(values);
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }
}
