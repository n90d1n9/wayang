package tech.kayys.wayang.agent.core.skills.executor;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.skills.SkillsLoader.SkillMetadata;

import java.util.*;

/**
 * Validates parameters for skill execution.
 * Ensures parameters match skill metadata requirements.
 */
public class SkillParameterValidator {

    private static final Logger LOGGER = Logger.getLogger(SkillParameterValidator.class);

    /**
     * Validate parameters against skill metadata.
     */
    public static ValidationResult validateParameters(String skillName, SkillMetadata metadata, 
                                                      Map<String, Object> parameters) {
        ValidationResult result = new ValidationResult();

        // Extract allowed parameters from metadata
        String allowedToolsJson = metadata.getAllowedTools();
        
        if (allowedToolsJson == null || allowedToolsJson.isEmpty()) {
            LOGGER.debugf("No parameter schema defined for skill: %s", skillName);
            return result; // No validation if no schema defined
        }

        // Parse JSON string to Map
        Map<String, Object> allowedTools;
        try {
            allowedTools = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(allowedToolsJson, java.util.HashMap.class);
        } catch (Exception e) {
            LOGGER.debugf("Failed to parse allowed tools schema for skill %s: %s", skillName, e.getMessage());
            return result; // Skip validation on parse error
        }

        if (allowedTools == null || allowedTools.isEmpty()) {
            return result; // No validation if schema is empty
        }

        // Check for required parameters
        for (Map.Entry<String, Object> entry : allowedTools.entrySet()) {
            String paramName = entry.getKey();
            Object paramSchema = entry.getValue();

            // Simple required check
            if (paramSchema instanceof Map) {
                Map<String, Object> schema = (Map<String, Object>) paramSchema;
                boolean required = (Boolean) schema.getOrDefault("required", false);

                if (required && !parameters.containsKey(paramName)) {
                    result.addError("Missing required parameter: " + paramName);
                }
            }
        }

        // Check for unexpected parameters
        for (String paramName : parameters.keySet()) {
            if (!allowedTools.containsKey(paramName)) {
                LOGGER.warnf("Parameter '%s' not defined in skill schema for %s", paramName, skillName);
                // Warning only, don't fail validation
            }
        }

        return result;
    }

    /**
     * Validation result with error tracking.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private boolean valid = true;

        public void addError(String error) {
            errors.add(error);
            valid = false;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return List.copyOf(errors);
        }

        @Override
        public String toString() {
            if (valid) {
                return "Valid";
            }
            return String.join("; ", errors);
        }
    }
}
