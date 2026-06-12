package tech.kayys.wayang.agent.skills.cli;

import java.util.Locale;

enum SkillsConfigSampleFormat {
    PROPERTIES,
    ENV;

    static SkillsConfigSampleFormat from(String value) {
        String normalized = value == null || value.isBlank()
                ? "properties"
                : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "properties", "property", "props" -> PROPERTIES;
            case "env", "environment", "dotenv" -> ENV;
            default -> throw new IllegalArgumentException(
                    "Unknown config sample format: " + value + " (expected properties or env)");
        };
    }
}
