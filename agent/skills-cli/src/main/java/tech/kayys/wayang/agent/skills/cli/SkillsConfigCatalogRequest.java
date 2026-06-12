package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for selecting runtime config hint catalog content.
 */
record SkillsConfigCatalogRequest(String groupName) {

    SkillsConfigCatalogRequest {
        groupName = groupName == null ? "" : groupName.trim();
    }

    static SkillsConfigCatalogRequest defaults() {
        return new SkillsConfigCatalogRequest("");
    }

    static SkillsConfigCatalogRequest forGroup(String groupName) {
        return new SkillsConfigCatalogRequest(groupName);
    }

    boolean hasGroup() {
        return !groupName.isBlank();
    }
}
