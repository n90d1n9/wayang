package tech.kayys.wayang.tenant.cas;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/access")
@RegisterRestClient(configKey = "wayang-cas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface CasAccessClient {

    @POST
    @Path("/validate-api-key")
    Uni<CasConsumerContext> validateApiKey(CasApiKeyValidationRequest request);
}
