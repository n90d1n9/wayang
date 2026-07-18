package tech.kayys.wayang.guardrails.policy;

import java.util.Map;

public interface ContentModerator {
    ModerationResult moderate(Map<String, Object> content);
}
