package tech.kayys.wayang.memory.integration;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceTypes;
import tech.kayys.wayang.memory.spi.AgentMemory;
import tech.kayys.wayang.memory.spi.MemoryEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Framework-neutral bridge that enriches active Wayang requests with relevant
 * memory context. Runtime adapters can call this service from HTTP, CLI,
 * Gamelan, or Gollek-specific boundaries without bringing those contracts into
 * the memory module itself.
 */
@ApplicationScoped
public class MemoryIntegrationService {

    static final String DEFAULT_AGENT_ID = "default-agent";
    static final String CONTEXT_HEADER = "Relevant memory context:";

    @Inject
    AgentMemory agentMemory;

    boolean enabled = true;
    int maxResults = 5;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = Math.max(0, maxResults);
    }

    public Uni<MemoryContextInjection> retrieveContext(String agentId, String query) {
        String resolvedAgentId = normalize(agentId, DEFAULT_AGENT_ID);
        if (!enabled || agentMemory == null || maxResults <= 0 || isBlank(query)) {
            return Uni.createFrom().item(emptyInjection(resolvedAgentId, query));
        }

        return agentMemory.retrieve(resolvedAgentId, query, maxResults)
                .map(entries -> injection(resolvedAgentId, query, entries))
                .onFailure().recoverWithItem(emptyInjection(resolvedAgentId, query));
    }

    public Uni<AgentRequest> augmentAgentRequest(AgentRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("request must not be null"));
        }

        String query = request.prompt();
        String agentId = resolveAgentId(request);
        if (!enabled || isBlank(query)) {
            return Uni.createFrom().item(request);
        }

        return retrieveContext(agentId, query)
                .map(injection -> injection.hasMemories() ? apply(request, injection) : request);
    }

    public Uni<InferenceRequest> augmentInferenceRequest(String agentId, InferenceRequest request) {
        if (request == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("request must not be null"));
        }

        String query = lastUserMessage(request.messages());
        String resolvedAgentId = resolveAgentId(agentId, request);
        if (!enabled || isBlank(query)) {
            return Uni.createFrom().item(request);
        }

        return retrieveContext(resolvedAgentId, query)
                .map(injection -> injection.hasMemories() ? apply(request, injection) : request);
    }

    private AgentRequest apply(AgentRequest request, MemoryContextInjection injection) {
        Map<String, Object> context = new LinkedHashMap<>(request.context());
        context.putAll(injection.contextVariables());

        return new AgentRequest(
                request.requestId(),
                request.prompt(),
                appendMemoryContext(request.systemPrompt(), injection.contextMessage()),
                request.strategy(),
                request.allowedSkills(),
                context,
                request.parameters(),
                request.tenantId(),
                request.sessionId(),
                request.userId(),
                request.stream(),
                request.verbose(),
                request.timeout(),
                request.memoryConfig(),
                request.modelId(),
                request.timestamp());
    }

    private InferenceRequest apply(InferenceRequest request, MemoryContextInjection injection) {
        List<InferenceTypes.ChatMessage> messages = new ArrayList<>();
        messages.add(new InferenceTypes.SystemMessage(injection.contextMessage()));
        messages.addAll(request.messages());

        Map<String, Object> metadata = new LinkedHashMap<>(request.metadata());
        metadata.putAll(injection.contextVariables());

        return new InferenceRequest(
                request.requestId(),
                request.model(),
                messages,
                request.tools(),
                request.toolChoice(),
                request.temperature(),
                request.maxTokens(),
                request.topP(),
                request.stopSequences(),
                request.stream(),
                request.timeout(),
                metadata);
    }

    private MemoryContextInjection injection(String agentId, String query, List<MemoryEntry> entries) {
        List<MemoryEntry> usableEntries = entries == null ? List.of() : entries.stream()
                .filter(entry -> entry != null && !isBlank(entry.content()))
                .limit(maxResults)
                .toList();
        return new MemoryContextInjection(agentId, query, usableEntries, buildContextMessage(usableEntries));
    }

    private MemoryContextInjection emptyInjection(String agentId, String query) {
        return new MemoryContextInjection(agentId, query, List.of(), "");
    }

    private String buildContextMessage(List<MemoryEntry> entries) {
        List<String> contents = entries.stream()
                .map(MemoryEntry::content)
                .filter(content -> content != null && !content.isBlank())
                .toList();
        if (contents.isEmpty()) {
            return "";
        }
        return CONTEXT_HEADER + "\n" + String.join("\n---\n", contents);
    }

    private String appendMemoryContext(String systemPrompt, String memoryContext) {
        if (isBlank(systemPrompt)) {
            return memoryContext;
        }
        return systemPrompt + "\n\n" + memoryContext;
    }

    private String resolveAgentId(AgentRequest request) {
        Object contextAgentId = request.context().get("agentId");
        String agentId = contextAgentId instanceof String value ? value : null;
        agentId = normalize(agentId, null);
        if (agentId != null) {
            return agentId;
        }
        agentId = normalize(request.sessionId(), null);
        if (agentId != null) {
            return agentId;
        }
        agentId = normalize(request.userId(), null);
        if (agentId != null) {
            return agentId;
        }
        return normalize(request.requestId(), DEFAULT_AGENT_ID);
    }

    private String resolveAgentId(String explicitAgentId, InferenceRequest request) {
        String agentId = normalize(explicitAgentId, null);
        if (agentId != null) {
            return agentId;
        }
        Object metadataAgentId = request.metadata().get("agentId");
        agentId = metadataAgentId instanceof String value ? normalize(value, null) : null;
        if (agentId != null) {
            return agentId;
        }
        return normalize(request.requestId(), DEFAULT_AGENT_ID);
    }

    private String lastUserMessage(List<InferenceTypes.ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            InferenceTypes.ChatMessage message = messages.get(i);
            if (message instanceof InferenceTypes.UserMessage userMessage) {
                return userMessage.content();
            }
        }
        return null;
    }

    private String normalize(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
