package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Policy for installing and evaluating Agentic Commerce Wayang bootstrap state.
 */
public record AgenticCommerceWayangBootstrapConfig(
        List<String> skillIds,
        boolean includeDefinitions,
        boolean includeRuntimeSkills,
        boolean requireSkillRegistration,
        boolean requireSmokeProbe,
        boolean requireBindingRoutes) {

    public AgenticCommerceWayangBootstrapConfig {
        skillIds = normalizeSkillIds(skillIds);
        if (!includeDefinitions && !includeRuntimeSkills) {
            throw new IllegalArgumentException("Agentic Commerce bootstrap must include definitions or runtime skills");
        }
    }

    public static AgenticCommerceWayangBootstrapConfig defaults() {
        return builder().build();
    }

    public static AgenticCommerceWayangBootstrapConfig definitionsOnly() {
        return builder()
                .includeRuntimeSkills(false)
                .build();
    }

    public static AgenticCommerceWayangBootstrapConfig runtimeSkillsOnly() {
        return builder()
                .includeDefinitions(false)
                .build();
    }

    public static AgenticCommerceWayangBootstrapConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Builder builder = builder();
        Object skillIds = AgenticCommerceWayangMaps.first(
                resolved,
                "skillIds",
                "skills",
                "selectedSkillIds",
                "checkoutSkillIds");
        if (skillIds != null) {
            builder.skillIds(AgenticCommerceWayangMaps.stringList(skillIds));
        }
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "includeDefinitions",
                "definitions",
                "registerDefinitions").ifPresent(builder::includeDefinitions);
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "includeRuntimeSkills",
                "runtimeSkills",
                "registerRuntimeSkills").ifPresent(builder::includeRuntimeSkills);
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "requireSkillRegistration",
                "requireSkills",
                "skillsRequired").ifPresent(builder::requireSkillRegistration);
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "requireSmokeProbe",
                "requireSmoke",
                "smokeRequired").ifPresent(builder::requireSmokeProbe);
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "requireBindingRoutes",
                "requireBinding",
                "bindingRequired").ifPresent(builder::requireBindingRoutes);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("skillIds", skillIds);
        values.put("includeDefinitions", includeDefinitions);
        values.put("includeRuntimeSkills", includeRuntimeSkills);
        values.put("requireSkillRegistration", requireSkillRegistration);
        values.put("requireSmokeProbe", requireSmokeProbe);
        values.put("requireBindingRoutes", requireBindingRoutes);
        return Map.copyOf(values);
    }

    private static List<String> normalizeSkillIds(List<String> skillIds) {
        List<String> normalized = AgenticCommerceWayangMaps.stringList(skillIds);
        return normalized.isEmpty() ? AgenticCommerceWayang.checkoutSkillIds() : normalized;
    }

    public static final class Builder {

        private List<String> skillIds = AgenticCommerceWayang.checkoutSkillIds();
        private boolean includeDefinitions = true;
        private boolean includeRuntimeSkills = true;
        private boolean requireSkillRegistration = true;
        private boolean requireSmokeProbe = true;
        private boolean requireBindingRoutes = true;

        private Builder() {
        }

        public Builder skillIds(List<String> skillIds) {
            this.skillIds = skillIds;
            return this;
        }

        public Builder includeDefinitions(boolean includeDefinitions) {
            this.includeDefinitions = includeDefinitions;
            return this;
        }

        public Builder includeRuntimeSkills(boolean includeRuntimeSkills) {
            this.includeRuntimeSkills = includeRuntimeSkills;
            return this;
        }

        public Builder requireSkillRegistration(boolean requireSkillRegistration) {
            this.requireSkillRegistration = requireSkillRegistration;
            return this;
        }

        public Builder requireSmokeProbe(boolean requireSmokeProbe) {
            this.requireSmokeProbe = requireSmokeProbe;
            return this;
        }

        public Builder requireBindingRoutes(boolean requireBindingRoutes) {
            this.requireBindingRoutes = requireBindingRoutes;
            return this;
        }

        public AgenticCommerceWayangBootstrapConfig build() {
            return new AgenticCommerceWayangBootstrapConfig(
                    skillIds,
                    includeDefinitions,
                    includeRuntimeSkills,
                    requireSkillRegistration,
                    requireSmokeProbe,
                    requireBindingRoutes);
        }
    }
}
