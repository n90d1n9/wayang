package tech.kayys.wayang.memory.integration;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceTypes;
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryIntegrationServiceTest {

    @Test
    void augmentsAgentRequestWithMemoryContext() {
        FakeAgentMemory memory = new FakeAgentMemory();
        memory.entries = List.of(
                entry("m1", "Deployment freezes start Friday."),
                entry("m2", "Use the blue environment for rehearsals."));
        MemoryIntegrationService service = service(memory);

        AgentRequest request = AgentRequest.builder()
                .requestId("req-1")
                .agentId("agent-a")
                .prompt("What should I remember for deployment?")
                .systemPrompt("Answer as the release coordinator.")
                .build();

        AgentRequest augmented = service.augmentAgentRequest(request).await().indefinitely();

        assertThat(memory.lastAgentId).isEqualTo("agent-a");
        assertThat(memory.lastQuery).isEqualTo("What should I remember for deployment?");
        assertThat(memory.lastLimit).isEqualTo(5);
        assertThat(augmented.systemPrompt())
                .contains("Answer as the release coordinator.")
                .contains(MemoryIntegrationService.CONTEXT_HEADER)
                .contains("Deployment freezes start Friday.")
                .contains("Use the blue environment for rehearsals.");
        assertThat(augmented.context())
                .containsEntry(MemoryContextInjection.MEMORY_AGENT_ID_KEY, "agent-a")
                .containsKey(MemoryContextInjection.RETRIEVED_MEMORIES_KEY)
                .containsKey(MemoryContextInjection.MEMORY_CONTEXT_MESSAGE_KEY);
    }

    @Test
    void augmentsInferenceRequestByPrependingSystemMemoryMessage() {
        FakeAgentMemory memory = new FakeAgentMemory();
        memory.entries = List.of(entry("m1", "The customer prefers concise status updates."));
        MemoryIntegrationService service = service(memory);

        InferenceRequest request = InferenceRequest.builder()
                .requestId("req-2")
                .metadata(Map.of("agentId", "agent-b"))
                .message(new InferenceTypes.SystemMessage("Base system prompt."))
                .message(new InferenceTypes.UserMessage("Draft the update."))
                .build();

        InferenceRequest augmented = service.augmentInferenceRequest(null, request).await().indefinitely();

        assertThat(memory.lastAgentId).isEqualTo("agent-b");
        assertThat(memory.lastQuery).isEqualTo("Draft the update.");
        assertThat(augmented.messages()).hasSize(3);
        assertThat(augmented.messages().getFirst())
                .isInstanceOf(InferenceTypes.SystemMessage.class)
                .extracting(InferenceTypes.ChatMessage::content)
                .asString()
                .contains(MemoryIntegrationService.CONTEXT_HEADER)
                .contains("concise status updates");
        assertThat(augmented.messages().get(1)).isEqualTo(request.messages().getFirst());
        assertThat(augmented.metadata())
                .containsEntry(MemoryContextInjection.MEMORY_AGENT_ID_KEY, "agent-b")
                .containsKey(MemoryContextInjection.RETRIEVED_MEMORIES_KEY);
    }

    @Test
    void leavesInferenceRequestUnchangedWhenNoUserMessageExists() {
        FakeAgentMemory memory = new FakeAgentMemory();
        MemoryIntegrationService service = service(memory);
        InferenceRequest request = InferenceRequest.builder()
                .requestId("req-3")
                .message(new InferenceTypes.SystemMessage("Base system prompt."))
                .build();

        InferenceRequest augmented = service.augmentInferenceRequest("agent-c", request).await().indefinitely();

        assertThat(augmented).isSameAs(request);
        assertThat(memory.retrieveCalls).isZero();
    }

    private MemoryIntegrationService service(FakeAgentMemory memory) {
        MemoryIntegrationService service = new MemoryIntegrationService();
        service.agentMemory = memory;
        service.enabled = true;
        service.maxResults = 5;
        return service;
    }

    private MemoryEntry entry(String id, String content) {
        return new MemoryEntry(id, content, Instant.parse("2026-01-02T03:04:05Z"), Map.of());
    }

    private static final class FakeAgentMemory implements AgentMemory {
        private List<MemoryEntry> entries = List.of();
        private int retrieveCalls;
        private String lastAgentId;
        private String lastQuery;
        private int lastLimit;

        @Override
        public Uni<Void> store(String agentId, MemoryEntry entry) {
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<List<MemoryEntry>> retrieve(String agentId, String query, int limit) {
            retrieveCalls++;
            lastAgentId = agentId;
            lastQuery = query;
            lastLimit = limit;
            return Uni.createFrom().item(new ArrayList<>(entries));
        }

        @Override
        public Uni<List<MemoryEntry>> getContext(String agentId) {
            return Uni.createFrom().item(List.of());
        }

        @Override
        public Uni<Void> clear(String agentId) {
            return Uni.createFrom().voidItem();
        }
    }
}
