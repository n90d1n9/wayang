package tech.kayys.wayang.agent.hermes;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Creates deterministic identities for Hermes-learned skills.
 */
public final class HermesSkillIdentityFactory {

    public HermesSkillIdentity fromTask(String task) {
        String clean = HermesText.oneLine(task);
        return new HermesSkillIdentity(
                skillId(task),
                title(clean),
                description(clean));
    }

    private static String skillId(String task) {
        String slug = slug(task);
        CRC32 crc = new CRC32();
        crc.update(task == null ? new byte[0] : task.getBytes(StandardCharsets.UTF_8));
        String suffix = Long.toUnsignedString(crc.getValue(), 36);
        return "hermes-" + slug + "-" + suffix;
    }

    private static String slug(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.isBlank()) {
            normalized = "learned-workflow";
        }
        return normalized.length() > 42 ? normalized.substring(0, 42).replaceAll("-+$", "") : normalized;
    }

    private static String title(String cleanTask) {
        if (cleanTask.isBlank()) {
            return "Hermes Learned Workflow";
        }
        return cleanTask.length() > 80 ? cleanTask.substring(0, 77) + "..." : cleanTask;
    }

    private static String description(String cleanTask) {
        if (cleanTask.isBlank()) {
            return "Learned Hermes workflow from a successful multi-step run.";
        }
        return "Learned Hermes workflow for: " + cleanTask;
    }
}
