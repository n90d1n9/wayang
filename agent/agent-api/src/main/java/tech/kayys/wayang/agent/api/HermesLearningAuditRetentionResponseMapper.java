package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;

import java.util.Optional;

/**
 * Maps the learning-audit port descriptor to a compact retention response.
 */
final class HermesLearningAuditRetentionResponseMapper {

    private final HermesPortResponseMapper responseMapper = new HermesPortResponseMapper();

    Response inspect(Optional<HermesLearningAuditPort> port) {
        if (port == null || port.isEmpty()) {
            return responseMapper.missingPort(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT);
        }
        try {
            HermesLearningAuditRetentionResponse body =
                    HermesLearningAuditRetentionResponse.from(port.orElseThrow().descriptor());
            Response.Status status = body.configured()
                    ? Response.Status.OK
                    : Response.Status.SERVICE_UNAVAILABLE;
            return Response.status(status)
                    .entity(body)
                    .build();
        } catch (RuntimeException error) {
            return Response.serverError()
                    .entity(ApiErrorResponse.from(error))
                    .build();
        }
    }
}
