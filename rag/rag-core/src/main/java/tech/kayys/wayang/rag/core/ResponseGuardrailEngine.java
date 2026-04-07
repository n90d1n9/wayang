package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.List;

@ApplicationScoped
class ResponseGuardrailEngine {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseGuardrailEngine.class);
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");

    public String validateAndSanitize(String response, GenerationConfig config) {
        String sanitized = response;

        // Remove PII
        sanitized = SSN_PATTERN.matcher(sanitized).replaceAll("[REDACTED-SSN]");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[REDACTED-EMAIL]");
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED-PHONE]");

        // Check for toxic content
        if (containsToxicContent(sanitized)) {
            LOG.warn("Potentially toxic content detected in response");
        }

        // Validate length
        int maxLength = config.maxTokens() * 4;
        if (sanitized.length() > maxLength) {
            LOG.warn("Response too long, truncating from {} to {}", sanitized.length(), maxLength);
            sanitized = sanitized.substring(0, maxLength) + "...";
        }

        return sanitized;
    }

    private boolean containsToxicContent(String text) {
        String lower = text.toLowerCase();
        List<String> toxicPatterns = List.of("offensive", "inappropriate");
        return toxicPatterns.stream().anyMatch(lower::contains);
    }
}
