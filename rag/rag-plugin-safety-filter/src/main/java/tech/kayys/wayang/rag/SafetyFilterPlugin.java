package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SafetyFilterPlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig;

    public SafetyFilterPlugin() {
    }

    public SafetyFilterPlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig;
    }

    @Override
    public String id() {
        return "safety-filter";
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public RagPluginExecutionContext beforeQuery(RagPluginExecutionContext context) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty()) {
            return context;
        }
        return context.withQuery(redact(context.query(), blocked, maskValue()));
    }

    @Override
    public List<RagScoredChunk> afterRetrieve(RagPluginExecutionContext context, List<RagScoredChunk> chunks) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty() || chunks == null || chunks.isEmpty()) {
            return chunks == null ? List.of() : chunks;
        }
        return chunks.stream()
                .filter(chunk -> !containsBlocked(chunk.chunk().text(), blocked))
                .collect(Collectors.toList());
    }

    @Override
    public RagResult afterResult(RagPluginExecutionContext context, RagResult result) {
        Set<String> blocked = blockedTerms();
        if (blocked.isEmpty() || result == null) {
            return result;
        }
        String answer = result.answer();
        String redacted = redact(answer, blocked, maskValue());
        if (answer == null || answer.equals(redacted)) {
            return result;
        }
        Map<String, Object> metadata = new HashMap<>();
        if (result.metadata() != null) {
            metadata.putAll(result.metadata());
        }
        metadata.put("plugin.safety_filter.answer_redacted", true);
        return new RagResult(result.query(), result.chunks(), redacted, Map.copyOf(metadata));
    }

    static boolean containsBlocked(String text, Set<String> blockedTerms) {
        if (text == null || text.isEmpty() || blockedTerms == null || blockedTerms.isEmpty()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return blockedTerms.stream().anyMatch(normalized::contains);
    }

    static String redact(String text, Set<String> blockedTerms, String mask) {
        if (text == null || text.isEmpty() || blockedTerms == null || blockedTerms.isEmpty()) {
            return text;
        }
        String effectiveMask = (mask == null || mask.isBlank()) ? "[REDACTED]" : mask;
        String redacted = text;
        for (String term : blockedTerms) {
            if (!term.isEmpty()) {
                redacted = redacted.replaceAll("(?i)" + java.util.regex.Pattern.quote(term), effectiveMask);
            }
        }
        return redacted;
    }

    private Set<String> blockedTerms() {
        String raw = tuningConfig.safetyFilterBlockedTerms();
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(token -> token.toLowerCase(Locale.ROOT))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    private String maskValue() {
        return tuningConfig.safetyFilterMask();
    }
}
