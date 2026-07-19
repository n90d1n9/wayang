package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
class CitationService {

    private static final Logger LOG = LoggerFactory.getLogger(CitationService.class);
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)\\]");
    private static final String UNKNOWN_SOURCE = "Unknown";

    public List<Citation> generateCitations(
            String response, List<String> contexts, List<Map<String, Object>> metadata) {
        LOG.debug("Response: {}", response);
        List<Citation> citations = new ArrayList<>();
        if (response == null || contexts == null || contexts.isEmpty()) {
            return citations;
        }

        Set<Integer> citedIndices = citedIndices(response);

        for (int i = 0; i < contexts.size(); i++) {
            String context = contexts.get(i);
            if (context == null || context.isBlank()) {
                continue;
            }

            if (citedIndices.contains(i + 1) || hasSignificantOverlap(response, context)) {
                Map<String, Object> meta = metadataAt(metadata, i);

                citations.add(new Citation(
                        extractSnippet(context),
                        sourcePath(meta),
                        i + 1,
                        meta));
            }
        }

        return citations;
    }

    private Set<Integer> citedIndices(String response) {
        Matcher matcher = CITATION_PATTERN.matcher(response);
        Set<Integer> citedIndices = new HashSet<>();
        while (matcher.find()) {
            try {
                int index = Integer.parseInt(matcher.group(1));
                if (index > 0) {
                    citedIndices.add(index);
                }
            } catch (NumberFormatException ignored) {
                LOG.debug("Ignoring out-of-range citation index: {}", matcher.group(1));
            }
        }
        return citedIndices;
    }

    private Map<String, Object> metadataAt(List<Map<String, Object>> metadata, int index) {
        if (metadata == null || index >= metadata.size()) {
            return Map.of();
        }
        return RagMetadata.copy(metadata.get(index));
    }

    private String sourcePath(Map<String, Object> metadata) {
        Object source = metadata.get("sourcePath");
        if (source == null) {
            return UNKNOWN_SOURCE;
        }
        String sourcePath = String.valueOf(source);
        return sourcePath.isBlank() ? UNKNOWN_SOURCE : sourcePath;
    }

    private boolean hasSignificantOverlap(String response, String context) {
        if (response.isBlank() || context.isBlank()) {
            return false;
        }
        String[] responseWords = response.toLowerCase(Locale.ROOT).split("\\s+");
        String[] contextWords = context.toLowerCase(Locale.ROOT).split("\\s+");

        Set<String> responseSet = new HashSet<>(Arrays.asList(responseWords));
        Set<String> contextSet = new HashSet<>(Arrays.asList(contextWords));

        responseSet.retainAll(contextSet);
        return responseSet.size() > responseWords.length * 0.3;
    }

    private String extractSnippet(String context) {
        return context.length() > 100 ? context.substring(0, 100) + "..." : context;
    }
}
