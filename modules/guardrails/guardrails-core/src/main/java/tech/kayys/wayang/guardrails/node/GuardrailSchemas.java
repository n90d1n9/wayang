package tech.kayys.wayang.guardrails.node;

public final class GuardrailSchemas {

    private GuardrailSchemas() {
    }

    public static final String GUARDRAIL_CONFIG = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "threshold": {
                  "type": "number",
                  "minimum": 0,
                  "maximum": 1,
                  "default": 0.5,
                  "description": "Sensitivity threshold for the detector"
                },
                "blocking": {
                  "type": "boolean",
                  "default": true,
                  "description": "Whether to block execution if issues are detected"
                },
                "redact": {
                  "type": "boolean",
                  "default": false,
                  "description": "Whether to redact sensitive content (e.g. for PII)"
                }
              }
            }
            """;

    public static final String GUARDRAIL_VALIDATE_CONFIG = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "detectors": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "List of detectors to run"
                },
                "failFast": {
                  "type": "boolean",
                  "default": false,
                  "description": "Whether to stop at the first violation"
                }
              }
            }
            """;
}
