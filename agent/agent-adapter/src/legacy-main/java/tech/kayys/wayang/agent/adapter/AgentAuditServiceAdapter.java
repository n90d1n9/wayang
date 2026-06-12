package tech.kayys.wayang.agent.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.spi.skills.audit.AuditService as GollekAuditService;
import tech.kayys.wayang.agent.spi.skills.audit.AuditEventType;
import tech.kayys.wayang.agent.spi.skills.audit.AuditStatus;
import tech.kayys.wayang.agent.spi.AgentAuditService;

import java.util.Map;

/**
 * Adapter that wraps gollek audit service for backward compatibility.
 * 
 * @deprecated Use {@link GollekAuditService} directly
 */
@ApplicationScoped
@Deprecated
public class AgentAuditServiceAdapter implements AgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditServiceAdapter.class);

    @Inject
    GollekAuditService gollekAuditService;

    @Override
    public void logEvent(String eventType, String userId, String skillId, String action, String status) {
        AuditEventType auditEventType = mapEventType(eventType);
        AuditStatus auditStatus = mapStatus(status);
        
        gollekAuditService.log(auditEventType, userId, skillId, action, auditStatus);
    }

    @Override
    public void logSuccess(String eventType, String userId, String skillId, String action) {
        gollekAuditService.logSuccess(mapEventType(eventType), userId, skillId, action);
    }

    @Override
    public void logFailure(String eventType, String userId, String skillId, String action, String error) {
        gollekAuditService.logFailure(mapEventType(eventType), userId, skillId, action, error);
    }

    @Override
    public void logAccessDenied(String userId, String skillId, String reason) {
        gollekAuditService.logAccessDenied(userId, skillId, reason);
    }

    private AuditEventType mapEventType(String eventType) {
        // Map wayang event types to gollek event types
        return switch (eventType.toUpperCase()) {
            case "SKILL_CREATED" -> AuditEventType.SKILL_CREATED;
            case "SKILL_UPDATED" -> AuditEventType.SKILL_UPDATED;
            case "SKILL_DELETED" -> AuditEventType.SKILL_DELETED;
            case "SKILL_EXECUTED" -> AuditEventType.SKILL_READ;
            default -> AuditEventType.SYSTEM_ERROR;
        };
    }

    private AuditStatus mapStatus(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> AuditStatus.SUCCESS;
            case "FAILURE" -> AuditStatus.FAILURE;
            case "DENIED" -> AuditStatus.DENIED;
            default -> AuditStatus.SUCCESS;
        };
    }
}
