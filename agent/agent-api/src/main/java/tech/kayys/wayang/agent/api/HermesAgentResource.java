package tech.kayys.wayang.agent.api;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;

/**
 * REST API for Hermes-specific operational ports.
 */
@Path("/api/v1/agents/hermes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HermesAgentResource {

    @Inject
    Instance<HermesRuntimeDiagnosticsPort> runtimeDiagnosticsPorts;

    @Inject
    Instance<HermesRuntimeJournalPort> runtimeJournalPorts;

    @Inject
    Instance<HermesLearningAuditPort> learningAuditPorts;

    HermesRuntimeDiagnosticsPort runtimeDiagnosticsPort;

    HermesRuntimeJournalPort runtimeJournalPort;

    HermesLearningAuditPort learningAuditPort;

    private final HermesOperationalService operationalService = new HermesOperationalService();

    @GET
    @Path("/status")
    public Response status() {
        return operationalService.status(ports());
    }

    @GET
    @Path("/diagnostics")
    public Response diagnostics(@BeanParam HermesDiagnosticsRequest request) {
        return operationalService.diagnostics(ports(), request);
    }

    @GET
    @Path("/capabilities")
    public Response capabilities() {
        return operationalService.capabilities(ports());
    }

    @GET
    @Path("/lifecycle")
    public Response lifecycle() {
        return operationalService.lifecycle(ports());
    }

    @GET
    @Path("/runtime-ports")
    public Response runtimePorts() {
        return operationalService.runtimePorts(ports());
    }

    @GET
    @Path("/skill-persistence")
    public Response skillPersistence() {
        return operationalService.skillPersistence(ports());
    }

    @GET
    @Path("/journal")
    public Response journal(@BeanParam HermesJournalRequest request) {
        return operationalService.journal(ports(), request);
    }

    @GET
    @Path("/journal/latest")
    public Response latestJournal(@BeanParam HermesJournalPresetRequest request) {
        return operationalService.latestJournal(ports(), request);
    }

    @GET
    @Path("/journal/failures")
    public Response failedJournal(@BeanParam HermesJournalPresetRequest request) {
        return operationalService.failedJournal(ports(), request);
    }

    @GET
    @Path("/journal/learning")
    public Response learningJournal(@BeanParam HermesJournalPresetRequest request) {
        return operationalService.learningJournal(ports(), request);
    }

    @GET
    @Path("/learning-audit")
    public Response learningAudit(@BeanParam HermesLearningAuditRequest request) {
        return operationalService.learningAudit(ports(), request);
    }

    @GET
    @Path("/learning-audit/diagnostics")
    public Response learningAuditDiagnostics() {
        return operationalService.learningAuditDiagnostics(ports());
    }

    @GET
    @Path("/learning-audit/retention")
    public Response learningAuditRetention() {
        return operationalService.learningAuditRetention(ports());
    }

    @GET
    @Path("/learning-audit/retention/events")
    public Response learningAuditRetentionEvents(@BeanParam HermesJournalRequest request) {
        return operationalService.learningAuditRetentionEvents(ports(), request);
    }

    @GET
    @Path("/learning-audit/latest")
    public Response latestLearningAudit(@BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.latestLearningAudit(ports(), request);
    }

    @GET
    @Path("/learning-audit/persisted")
    public Response persistedLearningAudit(@BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.persistedLearningAudit(ports(), request);
    }

    @GET
    @Path("/learning-audit/skills/{skillId}")
    public Response skillLearningAudit(
            @PathParam("skillId") String skillId,
            @BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.skillLearningAudit(ports(), skillId, request);
    }

    @GET
    @Path("/learning-audit/statuses/{status}")
    public Response statusLearningAudit(
            @PathParam("status") String status,
            @BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.statusLearningAudit(ports(), status, request);
    }

    @GET
    @Path("/learning-audit/outcomes/{outcome}")
    public Response outcomeLearningAudit(
            @PathParam("outcome") String outcome,
            @BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.outcomeLearningAudit(ports(), outcome, request);
    }

    @GET
    @Path("/learning-audit/receipts/{idempotencyKey}")
    public Response receiptLearningAudit(
            @PathParam("idempotencyKey") String idempotencyKey,
            @BeanParam HermesLearningAuditPresetRequest request) {
        return operationalService.receiptLearningAudit(ports(), idempotencyKey, request);
    }

    @GET
    @Path("/journal/requests/{requestId}")
    public Response requestJournal(
            @PathParam("requestId") String requestId,
            @BeanParam HermesJournalPresetRequest request) {
        return operationalService.requestJournal(ports(), requestId, request);
    }

    @GET
    @Path("/journal/sessions/{sessionId}")
    public Response sessionJournal(
            @PathParam("sessionId") String sessionId,
            @BeanParam HermesJournalPresetRequest request) {
        return operationalService.sessionJournal(ports(), sessionId, request);
    }

    @GET
    @Path("/journal/users/{userId}")
    public Response userJournal(
            @PathParam("userId") String userId,
            @BeanParam HermesJournalPresetRequest request) {
        return operationalService.userJournal(ports(), userId, request);
    }

    @GET
    @Path("/journal/tenants/{tenantId}")
    public Response tenantJournal(
            @PathParam("tenantId") String tenantId,
            @BeanParam HermesJournalPresetRequest request) {
        return operationalService.tenantJournal(ports(), tenantId, request);
    }

    private HermesOperationalPortSources ports() {
        return new HermesOperationalPortSources(
                runtimeDiagnosticsPort,
                runtimeDiagnosticsPorts,
                runtimeJournalPort,
                runtimeJournalPorts,
                learningAuditPort,
                learningAuditPorts);
    }
}
