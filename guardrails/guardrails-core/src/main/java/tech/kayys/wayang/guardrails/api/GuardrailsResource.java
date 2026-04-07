package tech.kayys.wayang.guardrails.api;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.guardrails.policy.Policy;
import tech.kayys.wayang.guardrails.policy.PolicyRepository;

import java.util.List;

@Path("/api/v1/guardrails/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GuardrailsResource {

    @Inject
    PolicyRepository policyRepository;

    @GET
    public Uni<List<Policy>> getAllPolicies(@HeaderParam("x-tenant-id") @DefaultValue("default") String tenantId) {
        return policyRepository.findAll(tenantId);
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getPolicy(@PathParam("id") String id) {
        return policyRepository.findById(id)
                .map(opt -> opt.map(policy -> Response.ok(policy).build())
                        .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build()));
    }

    @POST
    public Uni<Response> createPolicy(Policy policy) {
        return policyRepository.save(policy)
                .map(saved -> Response.status(Response.Status.CREATED).entity(saved).build());
    }

    @PUT
    @Path("/{id}")
    public Uni<Response> updatePolicy(@PathParam("id") String id, Policy policy) {
        // Simple update: ensure ID matches, then save mapping
        if (!id.equals(policy.id())) {
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST).entity("ID mismatch").build());
        }

        return policyRepository.findById(id)
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return policyRepository.save(policy)
                                .map(saved -> Response.ok(saved).build());
                    } else {
                        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
                    }
                });
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deletePolicy(@PathParam("id") String id) {
        return policyRepository.deleteById(id)
                .map(deleted -> deleted ? Response.noContent().build()
                        : Response.status(Response.Status.NOT_FOUND).build());
    }
}
