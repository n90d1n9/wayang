package tech.kayys.gollek.agent.skills.management.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.agent.skills.management.SkillManagementService;
import tech.kayys.gollek.agent.skills.repo.spi.SkillContent;
import tech.kayys.gollek.agent.skills.repo.spi.SkillMetadata;

import java.util.List;
import java.util.Map;

/**
 * REST API for skill management.
 */
@Path("/api/skills")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SkillManagementResource {

    private static final Logger log = LoggerFactory.getLogger(SkillManagementResource.class);

    @Inject
    SkillManagementService skillService;

    /**
     * List all skills.
     */
    @GET
    public Response listSkills(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            var result = skillService.listSkills(page, size).await().indefinitely();
            return Response.ok(result).build();
        } catch (Exception e) {
            log.error("Failed to list skills", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Get a skill by ID.
     */
    @GET
    @Path("/{id}")
    public Response getSkill(@PathParam("id") String skillId) {
        try {
            var result = skillService.getSkill(skillId).await().indefinitely();
            return result.map(skill -> Response.ok(skill).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Skill not found: " + skillId))
                            .build());
        } catch (Exception e) {
            log.error("Failed to get skill: {}", skillId, e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Create a new skill.
     */
    @POST
    public Response createSkill(SkillContent content) {
        try {
            SkillMetadata metadata = skillService.createSkill(content).await().indefinitely();
            return Response.status(Response.Status.CREATED).entity(metadata).build();
        } catch (SkillManagementService.SkillAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to create skill", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Update an existing skill.
     */
    @PUT
    @Path("/{id}")
    public Response updateSkill(@PathParam("id") String skillId, SkillContent content) {
        try {
            SkillMetadata metadata = skillService.updateSkill(skillId, content).await().indefinitely();
            return Response.ok(metadata).build();
        } catch (SkillManagementService.SkillNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to update skill: {}", skillId, e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Delete a skill.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteSkill(@PathParam("id") String skillId) {
        try {
            boolean deleted = skillService.deleteSkill(skillId).await().indefinitely();
            if (deleted) {
                return Response.noContent().build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Skill not found: " + skillId))
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to delete skill: {}", skillId, e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Search skills.
     */
    @GET
    @Path("/search")
    public Response searchSkills(
            @QueryParam("q") String query,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        try {
            var result = skillService.searchSkills(query, page, size).await().indefinitely();
            return Response.ok(result).build();
        } catch (Exception e) {
            log.error("Failed to search skills", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Get skills by category.
     */
    @GET
    @Path("/category/{category}")
    public Response getByCategory(@PathParam("category") String category) {
        try {
            List<SkillMetadata> skills = skillService.getSkillsByCategory(category)
                    .await().indefinitely();
            return Response.ok(skills).build();
        } catch (Exception e) {
            log.error("Failed to get skills by category", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Get skills by tags.
     */
    @GET
    @Path("/tags")
    public Response getByTags(
            @QueryParam("tags") List<String> tags,
            @QueryParam("matchAll") @DefaultValue("false") boolean matchAll) {
        
        try {
            List<SkillMetadata> skills = skillService.getSkillsByTags(tags, matchAll)
                    .await().indefinitely();
            return Response.ok(skills).build();
        } catch (Exception e) {
            log.error("Failed to get skills by tags", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Enable a skill.
     */
    @POST
    @Path("/{id}/enable")
    public Response enableSkill(@PathParam("id") String skillId) {
        try {
            boolean enabled = skillService.enableSkill(skillId).await().indefinitely();
            return Response.ok(Map.of("enabled", enabled)).build();
        } catch (Exception e) {
            log.error("Failed to enable skill: {}", skillId, e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Disable a skill.
     */
    @POST
    @Path("/{id}/disable")
    public Response disableSkill(@PathParam("id") String skillId) {
        try {
            boolean disabled = skillService.disableSkill(skillId).await().indefinitely();
            return Response.ok(Map.of("disabled", disabled)).build();
        } catch (Exception e) {
            log.error("Failed to disable skill: {}", skillId, e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Get repository statistics.
     */
    @GET
    @Path("/stats")
    public Response getStats() {
        try {
            var stats = skillService.getStats().await().indefinitely();
            return Response.ok(stats).build();
        } catch (Exception e) {
            log.error("Failed to get stats", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Get active repository.
     */
    @GET
    @Path("/repository")
    public Response getRepository() {
        try {
            String repoName = skillService.getActiveRepositoryName();
            return Response.ok(Map.of("repository", repoName)).build();
        } catch (Exception e) {
            log.error("Failed to get repository", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Switch repository.
     */
    @POST
    @Path("/repository/switch")
    public Response switchRepository(Map<String, String> request) {
        try {
            String repoName = request.get("repository");
            if (repoName == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Repository name is required"))
                        .build();
            }

            skillService.switchRepository(repoName).await().indefinitely();
            return Response.ok(Map.of("repository", repoName, "status", "switched")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Failed to switch repository", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Migrate skills between repositories.
     */
    @POST
    @Path("/repository/migrate")
    public Response migrateRepository(Map<String, String> request) {
        try {
            String from = request.get("from");
            String to = request.get("to");

            if (from == null || to == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Source and target repositories are required"))
                        .build();
            }

            var stats = skillService.migrateRepository(from, to).await().indefinitely();
            return Response.ok(stats).build();
        } catch (Exception e) {
            log.error("Failed to migrate repository", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Clear cache.
     */
    @POST
    @Path("/cache/clear")
    public Response clearCache() {
        try {
            skillService.clearCache().await().indefinitely();
            return Response.ok(Map.of("status", "cache cleared")).build();
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Batch create skills.
     */
    @POST
    @Path("/batch")
    public Response batchCreate(List<SkillContent> contents) {
        try {
            var result = skillService.createSkills(contents).await().indefinitely();
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (Exception e) {
            log.error("Failed to batch create skills", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * Batch delete skills.
     */
    @DELETE
    @Path("/batch")
    public Response batchDelete(List<String> skillIds) {
        try {
            var result = skillService.deleteSkills(skillIds).await().indefinitely();
            return Response.ok(result).build();
        } catch (Exception e) {
            log.error("Failed to batch delete skills", e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
