package tech.kayys.wayang.guardrails.policy;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class DefaultRedactor implements Redactor {
    @Override
    public Map<String, Object> redact(Map<String, Object> content) {
        return content == null ? Map.of() : content;
    }
}
