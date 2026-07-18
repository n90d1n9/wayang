package tech.kayys.wayang.guardrails.policy;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DefaultContentModerator implements ContentModerator {
    @Override
    public ModerationResult moderate(Map<String, Object> content) {
        return new ModerationResult(false, List.of());
    }
}
