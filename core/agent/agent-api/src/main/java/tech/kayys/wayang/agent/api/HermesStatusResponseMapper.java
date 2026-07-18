package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;
import tech.kayys.wayang.agent.hermes.HermesRuntimePortDescriptor;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;

import java.util.Optional;

/**
 * Maps Hermes operational ports to the compact status response.
 */
final class HermesStatusResponseMapper {

    Response status(
            Optional<HermesRuntimeDiagnosticsPort> diagnosticsPort,
            Optional<HermesRuntimeJournalPort> journalPort,
            Optional<HermesLearningAuditPort> learningAuditPort) {
        boolean journalConfigured = journalConfigured(journalPort);
        boolean learningAuditConfigured = learningAuditConfigured(learningAuditPort);
        boolean learningAuditReady = learningAuditReady(learningAuditPort);
        if (diagnosticsPort == null || diagnosticsPort.isEmpty()) {
            return unavailable(
                    journalConfigured,
                    learningAuditConfigured,
                    learningAuditReady,
                    HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT);
        }
        HermesRuntimeDiagnosticsPort port = diagnosticsPort.orElseThrow();
        try {
            HermesPortResponse diagnostics = HermesPortResponse.from(
                    port.inspect(HermesRuntimeDiagnosticsDirective.summary()));
            HermesStatusResponse body = HermesStatusResponse.from(
                    diagnostics,
                    diagnosticsConfigured(port),
                    journalConfigured,
                    learningAuditConfigured,
                    learningAuditReady);
            return response(body);
        } catch (RuntimeException error) {
            return unavailable(
                    journalConfigured,
                    learningAuditConfigured,
                    learningAuditReady,
                    ApiErrorResponse.from(error).error());
        }
    }

    private Response response(HermesStatusResponse body) {
        Response.Status status = body.ready()
                ? Response.Status.OK
                : Response.Status.SERVICE_UNAVAILABLE;
        return Response.status(status)
                .entity(body)
                .build();
    }

    private Response unavailable(
            boolean journalConfigured,
            boolean learningAuditConfigured,
            boolean learningAuditReady,
            String reason) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(HermesStatusResponse.unavailable(
                        journalConfigured,
                        learningAuditConfigured,
                        learningAuditReady,
                        reason))
                .build();
    }

    private boolean diagnosticsConfigured(HermesRuntimeDiagnosticsPort port) {
        return port != null && port.descriptor() != null && port.descriptor().configured();
    }

    private boolean journalConfigured(Optional<HermesRuntimeJournalPort> port) {
        return port != null
                && port.map(this::journalConfigured)
                        .orElse(false);
    }

    private boolean journalConfigured(HermesRuntimeJournalPort port) {
        return port != null && port.descriptor() != null && port.descriptor().configured();
    }

    private boolean learningAuditConfigured(Optional<HermesLearningAuditPort> port) {
        return port != null
                && port.map(this::learningAuditConfigured)
                        .orElse(false);
    }

    private boolean learningAuditConfigured(HermesLearningAuditPort port) {
        HermesRuntimePortDescriptor descriptor = descriptor(port);
        return descriptor != null && descriptor.configured() && !descriptor.noop();
    }

    private boolean learningAuditReady(Optional<HermesLearningAuditPort> port) {
        return port != null
                && port.map(this::learningAuditReady)
                        .orElse(false);
    }

    private boolean learningAuditReady(HermesLearningAuditPort port) {
        HermesRuntimePortDescriptor descriptor = descriptor(port);
        return learningAuditConfigured(port) && descriptor.ready();
    }

    private HermesRuntimePortDescriptor descriptor(HermesLearningAuditPort port) {
        return port == null ? null : port.descriptor();
    }
}
