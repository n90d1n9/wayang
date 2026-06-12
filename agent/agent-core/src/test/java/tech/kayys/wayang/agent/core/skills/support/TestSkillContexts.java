package tech.kayys.wayang.agent.core.skills.support;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestSkillContexts {

    private TestSkillContexts() {
    }

    public static SkillContext context(String skillId, SkillMetadata metadata) {
        Map<String, Object> agentContext = metadata == null ? Map.of() : Map.of(SkillContextKeys.KEY_METADATA, metadata);
        return SkillContext.builder()
                .skillId(skillId)
                .tenantId("tenant-test")
                .inputs(Map.of("input", "value"))
                .agentContext(agentContext)
                .workingMemory(Map.of())
                .runId("run-test")
                .stepNumber(1)
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    public static SkillMetadata metadata(String name, String description, String version, String... tags) {
        Map<String, String> customMetadata = new LinkedHashMap<>();
        if (version != null && !version.isBlank()) {
            customMetadata.put("version", version);
        }
        if (tags != null && tags.length > 0) {
            customMetadata.put("tags", String.join(",", tags));
        }
        return SkillMetadata.builder()
                .name(name)
                .description(description)
                .customMetadata(customMetadata)
                .build();
    }
}
