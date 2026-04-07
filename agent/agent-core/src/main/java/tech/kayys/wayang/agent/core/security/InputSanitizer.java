package tech.kayys.wayang.agent.core.security;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Input sanitization to prevent injection attacks and malicious input.
 */
public class InputSanitizer {
    
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE)\\b.*?;?)|" +
            "(-{2}|/\\*|\\*/)|(;\\s*(SELECT|INSERT|UPDATE|DELETE))",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
            "[;&|`$()\\[\\]<>]"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<|>|\\{|\\}|\\(|\\)|\"|\\'|;|&|\\|",
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "exec", "eval", "system", "runtime", "getRuntime", "processBuilder",
            "java.lang.Runtime", "java.io.File"
    );
    
    /**
     * Sanitize query input for agent execution.
     */
    public static String sanitizeQuery(String input) {
        if (input == null) {
            return "";
        }
        
        // Trim whitespace
        String sanitized = input.trim();
        
        // Check length
        if (sanitized.length() > 10000) {
            throw new SecurityException("Query exceeds maximum length (10000 chars)");
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Potential SQL injection detected in query");
        }
        
        // Check for command injection
        if (COMMAND_INJECTION_PATTERN.matcher(sanitized).find()) {
            throw new SecurityException("Potential command injection detected in query");
        }
        
        // Check for dangerous Java keywords
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (sanitized.toLowerCase().contains(keyword)) {
                throw new SecurityException("Dangerous keyword detected: " + keyword);
            }
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize parameters to prevent injection.
     */
    public static Map<String, Object> sanitizeParameters(Map<String, Object> params) {
        if (params == null) {
            return Map.of();
        }
        
        Map<String, Object> sanitized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = sanitizeKey(entry.getKey());
            Object value = sanitizeValue(entry.getValue());
            sanitized.put(key, value);
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize JSON-like strings for output (prevent XSS).
     */
    public static String sanitizeOutput(String output) {
        if (output == null) {
            return "";
        }
        
        // Replace dangerous characters with escaped versions
        return output
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }
    
    /**
     * Validate client ID format.
     */
    public static String validateClientId(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            throw new SecurityException("Client ID cannot be empty");
        }
        
        if (!clientId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new SecurityException("Invalid client ID format");
        }
        
        if (clientId.length() > 256) {
            throw new SecurityException("Client ID too long");
        }
        
        return clientId;
    }
    
    private static String sanitizeKey(String key) {
        if (key == null || !key.matches("^[a-zA-Z0-9_-]+$")) {
            throw new SecurityException("Invalid parameter key: " + key);
        }
        return key;
    }
    
    @SuppressWarnings("unchecked")
    private static Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.length() > 5000) {
                throw new SecurityException("Parameter value too long");
            }
            // Allow strings but don't escape here - escape on output
            return strValue;
        }
        
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        
        if (value instanceof Map) {
            return sanitizeParameters((Map<String, Object>) value);
        }
        
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            return list.stream().map(InputSanitizer::sanitizeValue).toList();
        }
        
        // For other types, convert to string and limit length
        String strValue = value.toString();
        if (strValue.length() > 1000) {
            throw new SecurityException("Parameter value too long");
        }
        
        return strValue;
    }
    
    /**
     * Security exception for sanitization failures.
     */
    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
        
        public SecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
