package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;

import java.util.Optional;

/**
 * Coordinates Hermes operational endpoint behavior outside the HTTP resource.
 */
final class HermesOperationalService {

    private final CdiInstanceResolver portResolver = new CdiInstanceResolver();
    private final HermesStatusResponseMapper statusMapper = new HermesStatusResponseMapper();
    private final HermesDiagnosticsResponseMapper diagnosticsResponseMapper = new HermesDiagnosticsResponseMapper();
    private final HermesJournalResponseMapper journalResponseMapper = new HermesJournalResponseMapper();
    private final HermesLearningAuditResponseMapper learningAuditResponseMapper =
            new HermesLearningAuditResponseMapper();
    private final HermesLearningAuditRetentionResponseMapper learningAuditRetentionResponseMapper =
            new HermesLearningAuditRetentionResponseMapper();
    private final HermesDiagnosticsDirectiveMapper diagnosticsMapper = new HermesDiagnosticsDirectiveMapper();
    private final HermesJournalDirectiveMapper journalMapper = new HermesJournalDirectiveMapper();
    private final HermesJournalPresetMapper journalPresetMapper = new HermesJournalPresetMapper();
    private final HermesLearningAuditDirectiveMapper learningAuditMapper = new HermesLearningAuditDirectiveMapper();
    private final HermesLearningAuditPresetMapper learningAuditPresetMapper =
            new HermesLearningAuditPresetMapper();

    Response status(HermesOperationalPortSources ports) {
        return statusMapper.status(diagnosticsPort(ports), journalPort(ports), learningAuditPort(ports));
    }

    Response diagnostics(
            HermesOperationalPortSources ports,
            HermesDiagnosticsRequest request) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.directive(request));
    }

    Response capabilities(HermesOperationalPortSources ports) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.capabilities());
    }

    Response lifecycle(HermesOperationalPortSources ports) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.lifecycle());
    }

    Response runtimePorts(HermesOperationalPortSources ports) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.runtimePorts());
    }

    Response skillPersistence(HermesOperationalPortSources ports) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.skillPersistence());
    }

    Response learningAuditDiagnostics(HermesOperationalPortSources ports) {
        return diagnosticsResponseMapper.inspect(
                diagnosticsPort(ports),
                diagnosticsMapper.learningAudit());
    }

    Response learningAuditRetention(HermesOperationalPortSources ports) {
        return learningAuditRetentionResponseMapper.inspect(learningAuditPort(ports));
    }

    Response learningAuditRetentionEvents(
            HermesOperationalPortSources ports,
            HermesJournalRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalMapper.learningAuditRetention(request));
    }

    Response journal(
            HermesOperationalPortSources ports,
            HermesJournalRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalMapper.directive(request));
    }

    Response latestJournal(
            HermesOperationalPortSources ports,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.latest(request));
    }

    Response failedJournal(
            HermesOperationalPortSources ports,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.failures(request));
    }

    Response learningJournal(
            HermesOperationalPortSources ports,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.learning(request));
    }

    Response learningAudit(
            HermesOperationalPortSources ports,
            HermesLearningAuditRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditMapper.directive(request));
    }

    Response latestLearningAudit(
            HermesOperationalPortSources ports,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.latest(request));
    }

    Response persistedLearningAudit(
            HermesOperationalPortSources ports,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.persisted(request));
    }

    Response skillLearningAudit(
            HermesOperationalPortSources ports,
            String skillId,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.skill(skillId, request));
    }

    Response statusLearningAudit(
            HermesOperationalPortSources ports,
            String status,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.status(status, request));
    }

    Response outcomeLearningAudit(
            HermesOperationalPortSources ports,
            String outcome,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.outcome(outcome, request));
    }

    Response receiptLearningAudit(
            HermesOperationalPortSources ports,
            String idempotencyKey,
            HermesLearningAuditPresetRequest request) {
        return learningAuditResponseMapper.inspect(
                learningAuditPort(ports),
                () -> learningAuditPresetMapper.receipt(idempotencyKey, request));
    }

    Response requestJournal(
            HermesOperationalPortSources ports,
            String requestId,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.request(requestId, request));
    }

    Response sessionJournal(
            HermesOperationalPortSources ports,
            String sessionId,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.session(sessionId, request));
    }

    Response userJournal(
            HermesOperationalPortSources ports,
            String userId,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.user(userId, request));
    }

    Response tenantJournal(
            HermesOperationalPortSources ports,
            String tenantId,
            HermesJournalPresetRequest request) {
        return journalResponseMapper.inspect(
                journalPort(ports),
                () -> journalPresetMapper.tenant(tenantId, request));
    }

    private Optional<HermesRuntimeDiagnosticsPort> diagnosticsPort(HermesOperationalPortSources ports) {
        if (ports == null) {
            return Optional.empty();
        }
        return portResolver.first(ports.runtimeDiagnosticsPort(), ports.runtimeDiagnosticsPorts());
    }

    private Optional<HermesRuntimeJournalPort> journalPort(HermesOperationalPortSources ports) {
        if (ports == null) {
            return Optional.empty();
        }
        return portResolver.first(ports.runtimeJournalPort(), ports.runtimeJournalPorts());
    }

    private Optional<HermesLearningAuditPort> learningAuditPort(HermesOperationalPortSources ports) {
        if (ports == null) {
            return Optional.empty();
        }
        return portResolver.first(ports.learningAuditPort(), ports.learningAuditPorts());
    }
}
