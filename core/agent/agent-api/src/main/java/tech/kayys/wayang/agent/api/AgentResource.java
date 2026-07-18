package tech.kayys.wayang.agent.api;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.core.core.AgentClient;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.List;
import java.util.Map;

/**
 * REST API over the active backend-agnostic agent contracts.
 */
@Path("/api/v1/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentResource {

    @Inject
    AgentClient agentClient;

    @Inject
    SkillRegistry skillRegistry;

    private final AgentRunRequestMapper runRequestMapper = new AgentRunRequestMapper();
    private final AgentRunResponseMapper runResponseMapper = new AgentRunResponseMapper();
    private final AgentSkillCatalogService skillCatalogService = new AgentSkillCatalogService();

    @POST
    @Path("/run")
    public Uni<Response> run(AgentRunRequest request) {
        try {
            AgentRequest agentRequest = runRequestMapper.toAgentRequest(request, false);
            return agentClient.execute(agentRequest)
                    .map(runResponseMapper::ok)
                    .onFailure().recoverWithItem(runResponseMapper::serverError);
        } catch (RuntimeException error) {
            return Uni.createFrom().item(runResponseMapper.badRequest(error));
        }
    }

    @POST
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> stream(AgentRunRequest request) {
        try {
            return agentClient.execute(runRequestMapper.toAgentRequest(request, true))
                    .onItem().transform(runResponseMapper::streamData)
                    .onFailure().recoverWithItem(runResponseMapper::streamError)
                    .toMulti();
        } catch (RuntimeException error) {
            return Multi.createFrom().item(runResponseMapper.streamError(error));
        }
    }

    @GET
    @Path("/skills")
    public List<SkillSummary> listSkills(@BeanParam AgentSkillsRequest request) {
        return skillCatalogService.listSkills(skillRegistry, request);
    }

    @GET
    @Path("/skills/{skillId}")
    public Response getSkill(@PathParam("skillId") String skillId) {
        return skillCatalogService.getSkill(skillRegistry, skillId)
                .map(skill -> Response.ok(skill).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Skill not found: " + skillId))
                        .build());
    }

    @GET
    @Path("/health")
    public AgentHealthResponse health() {
        return skillCatalogService.health(skillRegistry);
    }

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
            String id,
            String name,
            String description,
            String category,
            String version,
            int priority,
            boolean healthy,
            boolean runtime) {
    }

    public record AgentHealthResponse(
            String status,
            int totalSkills,
            int healthySkills,
            int unhealthySkills) {
    }
}
