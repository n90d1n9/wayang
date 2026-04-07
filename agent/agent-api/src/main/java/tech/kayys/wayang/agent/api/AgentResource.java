package tech.kayys.wayang.agent.core;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.core.AgentBuilder;
import tech.kayys.wayang.agent.core.spi.*;
import tech.kayys.wayang.agent.core.spi.DefaultSkillRegistry;
import tech.kayys.wayang.agent.core.spi.SkillHealth;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * REST API for the Gollek Agent System.
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code POST /api/v1/agents/run} — Execute an agent task</li>
 * <li>{@code POST /api/v1/agents/stream} — Execute with streaming response</li>
 * <li>{@code GET  /api/v1/agents/skills} — List registered skills</li>
 * <li>{@code GET  /api/v1/agents/skills/:id} — Get skill details</li>
 * <li>{@code GET  /api/v1/agents/health} — Agent system health</li>
 * </ul>
 *
 * @author Bhangun
 */
@Path("/api/v1/agents")
@Tag(name = "Agent System", description = "Gollek agentic reasoning and skill orchestration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentResource {

    private static final Logger LOG = Logger.getLogger(AgentResource.class);

    @Inject
    AgentBuilder agentBuilder;

    @Inject
    DefaultSkillRegistry skillRegistry;

    // ── Execute ───────────────────────────────────────────────────────────────

    /**
     * Execute an agent task synchronously.
     *
     * <p>Request body example:
     * <pre>{@code
     * {
     * "prompt": "Summarize recent AI research trends",
     * "strategy": "react",
     * "skills": ["web-search", "summarization"],
     * "maxSteps": 10,
     * "tenantId": "enterprise"
     * }
     * }</pre>
     */
    @POST
    @Path("/run")
    @Operation(summary = "Execute agent task", description = "Run an agentic task with specified skills and strategy")
    public Uni<Response> run(@Valid AgentRunRequest req) {
        LOG.infof("Agent run: strategy=%s, skills=%s, tenant=%s",
                req.strategy(), req.skills(), req.tenantId());

        AgentBuilder.FluentAgent agent = agentBuilder.newAgent()
                .withPrompt(req.prompt())
                .withMaxSteps(req.maxSteps() > 0 ? req.maxSteps() : 15)
                .forTenant(req.tenantId() != null ? req.tenantId() : "community");

        if (req.strategy() != null) {
            OrchestrationStrategy strategy = parseStrategy(req.strategy());
            agent.usingStrategy(strategy);
        }
        if (req.skills() != null && !req.skills().isEmpty()) {
            agent.usingSkills(req.skills());
        }
        if (req.systemPrompt() != null) {
            agent.withSystemPrompt(req.systemPrompt());
        }
        if (req.modelId() != null) {
            agent.withModel(req.modelId());
        }
        if (req.context() != null) {
            agent.withContext(req.context());
        }
        if (req.timeout() != null) {
            agent.withTimeout(Duration.parse(req.timeout()));
        }

        return agent.execute()
                .map(resp -> Response.ok(toApiResponse(resp)).build())
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "Agent run failed");
                    return Response.serverError()
                            .entity(Map.of("error", err.getMessage()))
                            .build();
                });
    }

    /**
     * Execute an agent task with Server-Sent Events streaming.
     */
    @POST
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Stream agent execution", description = "Execute agent task with real-time event streaming")
    public Multi<String> stream(@Valid AgentRunRequest req) {
        AgentRequest request = agentBuilder.newAgent()
                .withPrompt(req.prompt())
                .usingSkills(req.skills() != null ? req.skills() : List.of())
                .withMaxSteps(req.maxSteps() > 0 ? req.maxSteps() : 15)
                .forTenant(req.tenantId() != null ? req.tenantId() : "community")
                .streaming()
                .build();

        // Real streaming via orchestrator.stream() — simplified here
        return Multi.createFrom().emitter(emitter -> {
            agentBuilder.newAgent()
                    .withPrompt(req.prompt())
                    .execute()
                    .subscribe().with(
                            resp -> {
                                emitter.emit("data: " + resp.answer() + "\n\n");
                                emitter.complete();
                            },
                            err -> {
                                emitter.emit("error: " + err.getMessage() + "\n\n");
                                emitter.complete();
                            });
        });
    }

    // ── Skills ────────────────────────────────────────────────────────────────

    @GET
    @Path("/skills")
    @Operation(summary = "List skills", description = "Return all registered agent skills")
    public List<SkillSummary> listSkills(
            @QueryParam("category") String category) {
        return skillRegistry.listAll().stream()
                .filter(s -> category == null || s.category().name().equalsIgnoreCase(category))
                .map(s -> new SkillSummary(s.id(), s.name(), s.description(),
                        s.category().name(), s.version(), s.priority(), s.isHealthy()))
                .toList();
    }

    @GET
    @Path("/skills/{skillId}")
    @Operation(summary = "Get skill details")
    public Response getSkill(@PathParam("skillId") String skillId) {
        return skillRegistry.find(skillId)
                .map(s -> Response.ok(new SkillSummary(
                        s.id(), s.name(), s.description(),
                        s.category().name(), s.version(), s.priority(), s.isHealthy())).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Skill not found: " + skillId))
                        .build());
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GET
    @Path("/health")
    @Operation(summary = "Agent system health")
    public AgentHealthResponse health() {
        int total = skillRegistry.size();
        int healthy = (int) skillRegistry.listAll().stream().filter(s -> s.isHealthy()).count();
        return new AgentHealthResponse("UP", total, healthy, total - healthy);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record AgentRunRequest(
            String prompt,
            String systemPrompt,
            String strategy,
            List<String> skills,
            int maxSteps,
            String timeout,
            String tenantId,
            String userId,
            String sessionId,
            String modelId,
            Map<String, Object> context) {
    }

    public record SkillSummary(
            String id, String name, String description,
            String category, String version, int priority, boolean healthy) {
    }

    public record AgentHealthResponse(
            String status, int totalSkills, int healthySkills, int unhealthySkills) {
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrchestrationStrategy parseStrategy(String s) {
        for (OrchestrationStrategy strat : OrchestrationStrategy.values()) {
            if (strat.id.equalsIgnoreCase(s) || strat.name().equalsIgnoreCase(s))
                return strat;
        }
        return OrchestrationStrategy.REACT;
    }

    private Map<String, Object> toApiResponse(AgentResponse resp) {
        return Map.of(
                "runId", resp.runId(),
                "requestId", resp.requestId(),
                "answer", resp.answer(),
                "totalSteps", resp.totalSteps(),
                "successful", resp.successful(),
                "strategy", resp.strategy(),
                "durationMs", resp.durationMs());
    }
}
