package tech.kayys.wayang.rag.core;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QUERY EXPANSION SERVICE - FULL IMPLEMENTATION
 */
@ApplicationScoped
class QueryExpansionService {

    private static final Logger LOG = LoggerFactory.getLogger(QueryExpansionService.class);

    // Simple synonym map - in production, use WordNet or LLM-based expansion
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "refund", List.of("return", "reimbursement", "money back"),
            "shipping", List.of("delivery", "shipment", "transport"),
            "product", List.of("item", "goods", "merchandise"),
            "policy", List.of("rule", "regulation", "guideline"),
            "customer", List.of("client", "buyer", "consumer"));

    public List<String> expand(String query, int numVariations) {
        LOG.debug("Expanding query: {} (variations: {})", query, numVariations);

        List<String> expansions = new ArrayList<>();

        // Method 1: Add synonyms
        String synonymExpanded = expandWithSynonyms(query);
        if (!synonymExpanded.equals(query)) {
            expansions.add(synonymExpanded);
        }

        // Method 2: Reformulate if question
        if (numVariations > 1 && isQuestion(query)) {
            String reformulated = reformulateQuestion(query);
            if (!reformulated.equals(query)) {
                expansions.add(reformulated);
            }
        }

        return expansions.stream()
                .distinct()
                .limit(numVariations)
                .collect(Collectors.toList());
    }

    private String expandWithSynonyms(String query) {
        String[] words = query.toLowerCase().split("\\s+");
        StringBuilder expanded = new StringBuilder();

        for (String word : words) {
            List<String> syns = SYNONYMS.get(word);
            if (syns != null && !syns.isEmpty()) {
                expanded.append(word).append(" OR ").append(syns.get(0)).append(" ");
            } else {
                expanded.append(word).append(" ");
            }
        }

        return expanded.toString().trim();
    }

    private String reformulateQuestion(String query) {
        String lower = query.toLowerCase();

        if (lower.startsWith("what is")) {
            return query.replaceFirst("(?i)what is", "explain");
        } else if (lower.startsWith("how")) {
            return query.replaceFirst("(?i)how", "steps to");
        } else if (lower.startsWith("why")) {
            return query.replaceFirst("(?i)why", "reason for");
        }

        return query;
    }

    private boolean isQuestion(String query) {
        String lower = query.toLowerCase();
        return lower.startsWith("what") || lower.startsWith("how") ||
                lower.startsWith("why") || lower.startsWith("when") ||
                lower.startsWith("where") || lower.startsWith("who") ||
                query.endsWith("?");
    }
}