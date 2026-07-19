package tech.kayys.wayang.rag.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitationServiceTest {

    private final CitationService service = new CitationService();

    @Test
    void returnsEmptyCitationsForMissingResponseOrContexts() {
        assertThat(service.generateCitations(null, null, null)).isEmpty();
        assertThat(service.generateCitations("Use the cited source [1].", List.of(), null)).isEmpty();
    }

    @Test
    void skipsBlankContextsAndDefaultsMissingMetadata() {
        List<String> contexts = Arrays.asList(null, " ", "The runbook says deploy blue first.");
        List<Map<String, Object>> metadata = Collections.singletonList(null);

        List<Citation> citations = service.generateCitations("Use the runbook [3].", contexts, metadata);

        assertThat(citations).hasSize(1);
        Citation citation = citations.get(0);
        assertThat(citation.getIndex()).isEqualTo(3);
        assertThat(citation.getContent()).isEqualTo("The runbook says deploy blue first.");
        assertThat(citation.getSourceUri()).isEqualTo("Unknown");
    }

    @Test
    void extractsExplicitCitationMetadataSafely() {
        List<Citation> citations = service.generateCitations(
                "Deploy to blue [1].",
                List.of("Deploy to blue before promoting to green."),
                List.of(Map.of(
                        "sourcePath", 42,
                        "title", "Deployment Guide",
                        "pageNumber", 5,
                        "sectionTitle", "Blue Green",
                        "confidenceScore", 0.75f)));

        assertThat(citations).hasSize(1);
        Citation citation = citations.get(0);
        assertThat(citation.getSourceUri()).isEqualTo("42");
        assertThat(citation.getTitle()).isEqualTo("Deployment Guide");
        assertThat(citation.getPageNumber()).isEqualTo(5);
        assertThat(citation.getSectionTitle()).isEqualTo("Blue Green");
        assertThat(citation.getConfidenceScore()).isEqualTo(0.75f);
    }

    @Test
    void ignoresOutOfRangeCitationIndexes() {
        List<Citation> citations = service.generateCitations(
                "See source [999999999999999999999999].",
                List.of("No meaningful overlap here."),
                null);

        assertThat(citations).isEmpty();
    }
}
