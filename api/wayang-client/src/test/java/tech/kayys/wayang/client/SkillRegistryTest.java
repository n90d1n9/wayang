package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.skill.AgentRunSkillAssessment;
import tech.kayys.wayang.agent.skill.AgentRunSkillPreflight;
import tech.kayys.wayang.agent.skill.AgentSkill;
import tech.kayys.wayang.agent.skill.AgentSkillDescriptor;
import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.agent.skill.AgentSkillDiscoveryService;
import tech.kayys.wayang.agent.skill.AgentSkillQuery;
import tech.kayys.wayang.agent.skill.AgentSkillState;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillRegistryTest {

    @Test
    void normalizesDescriptorMetadataForDiscovery() {
        AgentSkillDescriptor descriptor = new AgentSkillDescriptor(
                " RAG.Search ",
                " RAG Search ",
                " Find relevant passages ",
                " Retrieval ",
                " RAG ",
                " ",
                List.of(" assistant-agent ", "", "assistant-agent"),
                List.of(" query ", "tenant"),
                List.of(" citations ", "answer"),
                List.of(" docs ", "docs"),
                Map.of(" endpoint ", " /rag/search ", "empty", " "));

        assertThat(descriptor.id()).isEqualTo("rag.search");
        assertThat(descriptor.name()).isEqualTo("RAG Search");
        assertThat(descriptor.category()).isEqualTo("Retrieval");
        assertThat(descriptor.source()).isEqualTo("rag");
        assertThat(descriptor.version()).isEqualTo("1.0.0");
        assertThat(descriptor.surfaceIds()).containsExactly("assistant-agent");
        assertThat(descriptor.inputKeys()).containsExactly("query", "tenant");
        assertThat(descriptor.outputKeys()).containsExactly("citations", "answer");
        assertThat(descriptor.tags()).containsExactly("docs");
        assertThat(descriptor.metadata()).containsExactly(Map.entry("endpoint", "/rag/search"));
        assertThat(descriptor.supportsSurface(" ASSISTANT-Agent ")).isTrue();
        assertThat(descriptor.hasInputKey("QUERY")).isTrue();
        assertThat(descriptor.hasTag("DOCS")).isTrue();
    }

    @Test
    void registersAndDiscoversSkillsInInsertionOrder() {
        SkillRegistry registry = SkillRegistry.create();
        RegisteredSkill rag = new RegisteredSkill(
                skillDescriptor("rag.search", "RAG Search", "Retrieval", "rag", List.of("assistant-agent"),
                        List.of("docs"), List.of("query"), List.of("citations")),
                AgentSkillState.ACTIVE,
                List.of("docs-search"));
        RegisteredSkill mcp = new RegisteredSkill(
                skillDescriptor("mcp.github", "GitHub MCP", "Tools", "mcp", List.of("coding-agent"),
                        List.of("git"), List.of("repository"), List.of("patch")),
                AgentSkillState.PREVIEW,
                List.of());

        registry.register(rag);
        registry.register(mcp);

        assertThat(registry.skillIds()).containsExactly("rag.search", "mcp.github");
        assertThat(registry.categories()).containsExactly("Retrieval", "Tools");
        assertThat(registry.sources()).containsExactly("rag", "mcp");
        assertThat(registry.find(" docs-search ")).contains(rag);
        assertThat(registry.discover(new AgentSkillQuery(
                " assistant-agent ",
                " retrieval ",
                " rag ",
                AgentSkillState.ACTIVE,
                null,
                " docs ",
                " QUERY ",
                " citations "))).containsExactly(rag);
        assertThat(registry.discover(new AgentSkillQuery(
                "coding-agent",
                null,
                "mcp",
                AgentSkillState.PREVIEW,
                null,
                null,
                null,
                null))).containsExactly(mcp);
    }

    @Test
    void rejectsDuplicateIdsAndAliases() {
        SkillRegistry registry = SkillRegistry.create();
        registry.register(new RegisteredSkill(
                skillDescriptor(
                        "rag.search", "RAG Search", "Retrieval", "rag", List.of(), List.of(), List.of(), List.of()),
                AgentSkillState.ACTIVE,
                List.of("docs")));

        assertThatThrownBy(() -> registry.register(new RegisteredSkill(
                skillDescriptor(
                        " RAG.Search ", "Duplicate", "Retrieval", "rag", List.of(), List.of(), List.of(), List.of()),
                AgentSkillState.ACTIVE,
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate Wayang skill id 'rag.search'");

        assertThatThrownBy(() -> registry.register(new RegisteredSkill(
                skillDescriptor("other", "Other", "Retrieval", "rag", List.of(), List.of(), List.of(), List.of()),
                AgentSkillState.ACTIVE,
                List.of(" docs "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate Wayang skill id or alias 'docs'");

        assertThatThrownBy(() -> registry.register(new RegisteredSkill(
                skillDescriptor("docs", "Alias Id", "Retrieval", "rag", List.of(), List.of(), List.of(), List.of()),
                AgentSkillState.ACTIVE,
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate Wayang skill id or alias 'docs'");
    }

    @Test
    void returnsImmutableSnapshotsAndCanUnregisterByAlias() {
        SkillRegistry registry = SkillRegistry.of(List.of(new RegisteredSkill(
                skillDescriptor(
                        "rag.search", "RAG Search", "Retrieval", "rag", List.of(), List.of(), List.of(), List.of()),
                AgentSkillState.ACTIVE,
                List.of("docs"))));

        assertThatThrownBy(() -> registry.list().add(RegisteredSkill.active(
                skillDescriptor("new", "New", "General", "custom", List.of(), List.of(), List.of(), List.of()))))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(registry.unregister(" docs ")).isTrue();
        assertThat(registry.size()).isZero();
        assertThat(registry.unregister("missing")).isFalse();
    }

    @Test
    void acceptsCustomAgentSkillImplementations() {
        AgentSkill custom = new AgentSkill() {
            @Override
            public AgentSkillDescriptor descriptor() {
                return skillDescriptor("custom.audit", "Audit", "Governance", "custom",
                        List.of("operator-console"), List.of("audit"), List.of(), List.of());
            }

            @Override
            public AgentSkillState state() {
                return AgentSkillState.PREVIEW;
            }

            @Override
            public List<String> aliases() {
                return List.of("audit-log");
            }
        };

        SkillRegistry registry = SkillRegistry.of(List.of(custom));

        assertThat(registry.require("audit-log").id()).isEqualTo("custom.audit");
        assertThat(registry.require("custom.audit").state()).isEqualTo(AgentSkillState.PREVIEW);
    }

    @Test
    void defaultCatalogSeedsWayangProductCapabilities() {
        SkillRegistry registry = WayangSkillCatalog.defaultRegistry();

        assertThat(WayangSkillCatalog.defaultAvailableSkillCount()).isEqualTo(12);
        assertThat(registry.skillIds())
                .contains("repo.context", "rag.retrieve", "mcp.bridge", "workflow.gamelan", "skills.management");
        assertThat(registry.require("rag").id()).isEqualTo("rag.retrieve");
        assertThat(registry.discover(new AgentSkillQuery(
                "assistant-agent",
                null,
                "rag",
                AgentSkillState.ACTIVE,
                null,
                "docs",
                "query",
                "citations")))
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.id()).isEqualTo("rag.retrieve");
                    assertThat(skill.descriptor().surfaceIds()).contains("assistant-agent");
                });
        assertThat(registry.discover(AgentSkillQuery.forProfile("low-code-agent", null, null)))
                .extracting(RegisteredSkill::id)
                .containsExactly("workflow.gamelan", "hitl.approval", "observability.traces");
    }

    @Test
    void discoveryEnvelopeReportsFacetSummariesAndSearch() {
        AgentSkillDiscovery discovery = AgentSkillDiscoveryService.create().discover(
                WayangSkillCatalog.defaultRegistry(),
                new AgentSkillQuery(
                        "assistant-agent",
                        null,
                        null,
                        AgentSkillState.ACTIVE,
                        null,
                        null,
                        null,
                        null),
                "rag");

        assertThat(discovery.search()).isEqualTo("rag");
        assertThat(discovery.totalSkills()).isEqualTo(12);
        assertThat(discovery.matchingSkills()).isEqualTo(1);
        assertThat(discovery.skillIds()).containsExactly("rag.retrieve");
        assertThat(discovery.categories()).containsExactly("Retrieval");
        assertThat(discovery.categoryCounts()).containsExactly(Map.entry("Retrieval", 1));
        assertThat(discovery.categorySummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("Retrieval");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.skillIds()).containsExactly("rag.retrieve");
                });
        assertThat(discovery.sources()).containsExactly("rag");
        assertThat(discovery.sourceCounts()).containsExactly(Map.entry("rag", 1));
        assertThat(discovery.sourceSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("rag");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.skillIds()).containsExactly("rag.retrieve");
                });
    }

    @Test
    void profileSkillDiscoveryResolvesSurfaceAndDefaultSkillBundle() {
        AgentSkillQuery query = AgentSkillQuery.forProfile(" low-code-agent ", null, null);

        assertThat(query.profileId()).isEqualTo("low-code-agent");
        assertThat(query.surfaceId()).isEmpty();
        assertThat(query.resolvedSurfaceId()).isEqualTo("workflow-platform");

        AgentSkillDiscovery discovery = AgentSkillDiscoveryService.create().discover(
                WayangSkillCatalog.defaultRegistry(),
                query,
                "gamelan");

        assertThat(discovery.query().profileId()).isEqualTo("low-code-agent");
        assertThat(discovery.query().resolvedSurfaceId()).isEqualTo("workflow-platform");
        assertThat(discovery.totalSkills()).isEqualTo(12);
        assertThat(discovery.skillIds()).containsExactly("workflow.gamelan");
        assertThat(discovery.categories()).containsExactly("Workflow");
    }

    @Test
    void profileSkillDiscoveryRejectsSurfaceConflicts() {
        AgentSkillQuery query = new AgentSkillQuery(
                "assistant-agent",
                "low-code-agent",
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> WayangSkillCatalog.defaultRegistry().discover(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wayang product profile 'low-code-agent'")
                .hasMessageContaining("belongs to surface 'workflow-platform'")
                .hasMessageContaining("not 'assistant-agent'");
    }

    @Test
    void runSkillPreflightFlagsUnknownUnavailableAndSurfaceIncompatibleSkills() {
        SkillRegistry registry = SkillRegistry.of(List.of(
                new RegisteredSkill(
                        skillDescriptor("repo.context", "Repo", "Context", "builtin",
                                List.of("coding-agent"), List.of("repo"), List.of(), List.of()),
                        AgentSkillState.ACTIVE,
                        List.of("repo")),
                new RegisteredSkill(
                        skillDescriptor("legacy.skill", "Legacy", "General", "builtin",
                                List.of("assistant-agent"), List.of(), List.of(), List.of()),
                        AgentSkillState.DEPRECATED,
                        List.of("legacy"))));

        AgentRunSkillAssessment assessment = AgentRunSkillPreflight.assess(
                registry,
                AgentRunRequest.builder()
                        .surfaceId("assistant-agent")
                        .skill("repo")
                        .skill("legacy")
                        .skill("missing")
                        .build());

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.requestedSkills()).containsExactly("repo", "legacy", "missing");
        assertThat(assessment.resolvedSkillIds()).containsExactly("repo.context", "legacy.skill");
        assertThat(assessment.unknownSkills()).containsExactly("missing");
        assertThat(assessment.unavailableSkillIds()).containsExactly("legacy.skill");
        assertThat(assessment.incompatibleSkillIds()).containsExactly("repo.context");
        assertThat(assessment.recommendations())
                .contains(
                        "Register or remove unknown skills: missing.",
                        "Use active or preview skills instead of unavailable skills: legacy.skill.",
                        "Choose skills that support surface 'assistant-agent': repo.context.");
    }

    private static AgentSkillDescriptor skillDescriptor(
            String id,
            String name,
            String category,
            String source,
            List<String> surfaces,
            List<String> tags,
            List<String> inputs,
            List<String> outputs) {
        return new AgentSkillDescriptor(
                id,
                name,
                "description",
                category,
                source,
                "1.0.0",
                surfaces,
                inputs,
                outputs,
                tags,
                Map.of());
    }
}
