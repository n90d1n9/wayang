package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for rendering starter skill persistence runtime config.
 */
record SkillsConfigSampleRequest(
        String profileName,
        String formatName) {

    SkillsConfigSampleRequest {
        profileName = profileName == null ? "" : profileName.trim();
        formatName = formatName == null ? "" : formatName.trim();
    }

    static SkillsConfigSampleRequest defaults() {
        return new SkillsConfigSampleRequest("default", "properties");
    }

    static SkillsConfigSampleRequest fromOptions(String profileName, String formatName) {
        return new SkillsConfigSampleRequest(profileName, formatName);
    }

    SkillsConfigSampleFormat format() {
        return SkillsConfigSampleFormat.from(formatName);
    }
}
