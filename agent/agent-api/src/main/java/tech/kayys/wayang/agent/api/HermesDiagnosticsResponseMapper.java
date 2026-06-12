package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;

import java.util.Optional;

/**
 * Maps Hermes diagnostics-port inspections to HTTP responses.
 */
final class HermesDiagnosticsResponseMapper {

    private final HermesPortResponseMapper responseMapper = new HermesPortResponseMapper();

    Response inspect(
            Optional<HermesRuntimeDiagnosticsPort> port,
            HermesRuntimeDiagnosticsDirective directive) {
        return port
                .map(runtimePort -> responseMapper.dispatch(
                        () -> runtimePort.inspect(directive),
                        HermesDiagnosticsResponse::from))
                .orElseGet(() -> responseMapper.missingPort(
                        HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT));
    }
}
