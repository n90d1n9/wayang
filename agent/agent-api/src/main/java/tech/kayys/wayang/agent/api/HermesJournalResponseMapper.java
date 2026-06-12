package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalDirective;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Maps Hermes journal-port inspections to HTTP responses.
 */
final class HermesJournalResponseMapper {

    private final HermesPortResponseMapper responseMapper = new HermesPortResponseMapper();

    Response inspect(
            Optional<HermesRuntimeJournalPort> port,
            HermesRuntimeJournalDirective directive) {
        return inspect(port, () -> directive);
    }

    Response inspect(
            Optional<HermesRuntimeJournalPort> port,
            Supplier<HermesRuntimeJournalDirective> directive) {
        return port
                .map(runtimePort -> responseMapper.dispatch(
                        () -> runtimePort.inspect(directive.get()),
                        HermesJournalResponse::from))
                .orElseGet(() -> responseMapper.missingPort(
                        HermesOperationalMessages.MISSING_JOURNAL_PORT));
    }
}
