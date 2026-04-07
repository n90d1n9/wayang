package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
class CitationService {

    private static final Logger LOG = LoggerFactory.getLogger(CitationService.class);

    public List<Citation> generateCitations(
            String response, List<String> contexts, List<Map<String, Object>> metadata) {
        LOG.debug("Respons: ", response);
        List<Citation> citations = new ArrayList<>();
        Pattern citationPattern = Pattern.compile("\\[(\\d+)\\]");
        Matcher matcher = citationPattern.matcher(response);

        Set<Integer> citedIndices = new HashSet<>();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            citedIndices.add(index);
        }

        for (int i = 0; i < contexts.size(); i++) {
            if (citedIndices.contains(i + 1) || hasSignificantOverlap(response, contexts.get(i))) {
                Map<String, Object> meta = i < metadata.size() ? metadata.get(i) : Map.of();
                String source = (String) meta.getOrDefault("sourcePath", "Unknown");

                citations.add(new Citation(
                        extractSnippet(contexts.get(i)),
                        source,
                        i + 1,
                        meta));
            }
        }

        return citations;
    }

    private boolean hasSignificantOverlap(String response, String context) {
        String[] responseWords = response.toLowerCase().split("\\s+");
        String[] contextWords = context.toLowerCase().split("\\s+");

        Set<String> responseSet = new HashSet<>(Arrays.asList(responseWords));
        Set<String> contextSet = new HashSet<>(Arrays.asList(contextWords));

        responseSet.retainAll(contextSet);
        return responseSet.size() > responseWords.length * 0.3;
    }

    private String extractSnippet(String context) {
        return context.length() > 100 ? context.substring(0, 100) + "..." : context;
    }
}
