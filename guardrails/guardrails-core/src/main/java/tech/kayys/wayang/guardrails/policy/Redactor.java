package tech.kayys.wayang.guardrails.policy;

import java.util.Map;

public interface Redactor {
    Map<String, Object> redact(Map<String, Object> content);
}
