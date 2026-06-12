package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditDirective;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Maps Hermes learning-audit port inspections to HTTP responses.
 */
final class HermesLearningAuditResponseMapper {

    private final HermesPortResponseMapper responseMapper = new HermesPortResponseMapper();

    Response inspect(
            Optional<HermesLearningAuditPort> port,
            HermesLearningAuditDirective directive) {
        return inspect(port, () -> directive);
    }

    Response inspect(
            Optional<HermesLearningAuditPort> port,
            Supplier<HermesLearningAuditDirective> directive) {
        return port
                .map(runtimePort -> responseMapper.dispatch(
                        () -> runtimePort.inspect(directive.get()),
                        HermesLearningAuditResponse::from))
                .orElseGet(() -> responseMapper.missingPort(
                        HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT));
    }
}
