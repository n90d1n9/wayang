package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.agent.skill.AgentSkillDiscoveryService;
import tech.kayys.wayang.agent.skill.AgentSkillEnvelopes;
import tech.kayys.wayang.agent.skill.AgentSkillFacetSummary;
import tech.kayys.wayang.agent.skill.AgentSkillQuery;
import tech.kayys.wayang.agent.skill.AgentSkillState;
import tech.kayys.wayang.skill.RegisteredSkill;
import tech.kayys.wayang.skill.WayangSkillCatalog;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSkillEnvelopesTest {

    @Test
    void discoveryEnvelopeOwnsPublishedSkillShape() {
        AgentSkillDiscovery discovery = AgentSkillDiscoveryService.create().discover(
                WayangSkillCatalog.defaultRegistry(),
                new AgentSkillQuery(
                        " assistant-agent ",
                        null,
                        null,
                        AgentSkillState.ACTIVE,
                        null,
                        null,
                        null,
                        null),
                " rag ");

        Map<String, Object> values = AgentSkillEnvelopes.discovery(" Wayang ", discovery);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("search", "rag")
                .containsEntry("totalSkills", 12)
                .containsEntry("matchingSkills", 1)
                .containsEntry("categories", List.of("Retrieval"))
                .containsEntry("categoryCounts", Map.of("Retrieval", 1))
                .containsEntry("sources", List.of("rag"))
                .containsEntry("sourceCounts", Map.of("rag", 1))
                .containsEntry("skillIds", List.of("rag.retrieve"));

        assertThat(objectMap(values.get("query")))
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("profileId", null)
                .containsEntry("resolvedSurfaceId", "assistant-agent")
                .containsEntry("category", null)
                .containsEntry("source", null)
                .containsEntry("state", "ACTIVE")
                .containsEntry("skillId", null)
                .containsEntry("tag", null)
                .containsEntry("inputKey", null)
                .containsEntry("outputKey", null)
                .containsEntry("filtered", true);
        assertThat(list(values.get("categorySummaries")))
                .singleElement()
                .satisfies(summary -> assertThat(objectMap(summary))
                        .containsEntry("name", "Retrieval")
                        .containsEntry("count", 1)
                        .containsEntry("skillIds", List.of("rag.retrieve")));
        assertThat(list(values.get("skills")))
                .singleElement()
                .satisfies(skill -> assertThat(objectMap(skill))
                        .containsEntry("id", "rag.retrieve")
                        .containsEntry("source", "rag")
                        .containsEntry("state", "ACTIVE")
                        .containsEntry("availableForRuns", true)
                        .containsEntry("aliases", List.of("rag")));
    }

    @Test
    void detailEnvelopeUsesRegisteredSkillMap() {
        RegisteredSkill skill = WayangSkillCatalog.defaultRegistry().require("rag");

        Map<String, Object> values = AgentSkillEnvelopes.detail("Wayang", skill);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("skillId", "rag.retrieve");
        assertThat(objectMap(values.get("skill")))
                .containsEntry("id", "rag.retrieve")
                .containsEntry("name", "RAG Retrieval")
                .containsEntry("category", "Retrieval")
                .containsEntry("source", "rag")
                .containsEntry("inputKeys", List.of("query", "collection", "filters"))
                .containsEntry("outputKeys", List.of("chunks", "citations"));
    }

    @Test
    void queryAndFacetSummaryNormalizeForJsonContracts() {
        Map<String, Object> query = AgentSkillEnvelopes.query(new AgentSkillQuery(
                " ASSISTANT-Agent ",
                null,
                " Retrieval ",
                " RAG ",
                AgentSkillState.PREVIEW,
                " RAG.Retrieve ",
                " Docs ",
                " QUERY ",
                " citations "));
        Map<String, Object> summary = AgentSkillEnvelopes.facetSummary(
                new AgentSkillFacetSummary(" rag ", 2, List.of("one", "two")));

        assertThat(query)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("profileId", null)
                .containsEntry("resolvedSurfaceId", "assistant-agent")
                .containsEntry("category", "Retrieval")
                .containsEntry("source", "rag")
                .containsEntry("state", "PREVIEW")
                .containsEntry("skillId", "rag.retrieve")
                .containsEntry("tag", "docs")
                .containsEntry("inputKey", "QUERY")
                .containsEntry("outputKey", "citations")
                .containsEntry("filtered", true);
        assertThat(summary)
                .containsEntry("name", "rag")
                .containsEntry("count", 2)
                .containsEntry("skillIds", List.of("one", "two"));
    }

    @Test
    void nullDiscoveryProducesEmptyDiscoveryEnvelope() {
        Map<String, Object> values = AgentSkillEnvelopes.discovery(null, null);

        assertThat(values)
                .containsEntry("product", "")
                .containsEntry("search", null)
                .containsEntry("totalSkills", 0)
                .containsEntry("matchingSkills", 0)
                .containsEntry("skillIds", List.of())
                .containsEntry("skills", List.of());
        assertThat(objectMap(values.get("query"))).containsEntry("filtered", false);
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizeProvidesEmptySkillDiscoveryModel() {
        AgentSkillDiscovery model = AgentSkillEnvelopes.normalize(null);

        assertThat(model.query()).isEqualTo(AgentSkillQuery.all());
        assertThat(model.search()).isEmpty();
        assertThat(model.totalSkills()).isZero();
        assertThat(model.matchingSkills()).isZero();
        assertThat(model.skills()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
