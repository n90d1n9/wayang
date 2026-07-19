package tech.kayys.wayang.agent.history;

import tech.kayys.wayang.agent.run.AgentRunMetadata;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStates;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.client.SdkText;

public record AgentRunHistoryQuery(
        AgentRunState state,
        int limit,
        String tenantId,
        String sessionId,
        String surfaceId,
        String profileId,
        int offset) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public AgentRunHistoryQuery {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
        limit = Math.min(limit, MAX_LIMIT);
        tenantId = SdkText.trimToEmpty(tenantId);
        sessionId = SdkText.trimToEmpty(sessionId);
        surfaceId = SdkText.trimToEmpty(surfaceId);
        profileId = SdkText.trimToEmpty(profileId);
        offset = Math.max(0, offset);
    }

    public AgentRunHistoryQuery(
            AgentRunState state,
            int limit,
            String tenantId,
            String sessionId,
            String surfaceId,
            int offset) {
        this(state, limit, tenantId, sessionId, surfaceId, "", offset);
    }

    public AgentRunHistoryQuery(
            AgentRunState state,
            int limit,
            String tenantId,
            String sessionId,
            String surfaceId) {
        this(state, limit, tenantId, sessionId, surfaceId, "", 0);
    }

    public AgentRunHistoryQuery(AgentRunState state, int limit) {
        this(state, limit, "", "", "", "", 0);
    }

    public static AgentRunHistoryQuery all() {
        return new AgentRunHistoryQuery(null, DEFAULT_LIMIT);
    }

    public static AgentRunHistoryQuery of(String state, Integer limit) {
        return of(state, limit, "", "", "");
    }

    public static AgentRunHistoryQuery of(
            String state,
            Integer limit,
            String tenantId,
            String sessionId,
            String surfaceId) {
        return of(state, limit, tenantId, sessionId, surfaceId, "", null);
    }

    public static AgentRunHistoryQuery of(
            String state,
            Integer limit,
            String tenantId,
            String sessionId,
            String surfaceId,
            Integer offset) {
        return of(state, limit, tenantId, sessionId, surfaceId, "", offset);
    }

    public static AgentRunHistoryQuery of(
            String state,
            Integer limit,
            String tenantId,
            String sessionId,
            String surfaceId,
            String profileId,
            Integer offset) {
        return new AgentRunHistoryQuery(
                parseState(state),
                limit == null ? DEFAULT_LIMIT : limit,
                tenantId,
                sessionId,
                surfaceId,
                profileId,
                offset == null ? 0 : offset);
    }

    public boolean filtered() {
        return state != null
                || !tenantId.isBlank()
                || !sessionId.isBlank()
                || !surfaceId.isBlank()
                || !profileId.isBlank()
                || offset > 0;
    }

    public boolean matches(AgentRunStatus status) {
        if (status == null) {
            return false;
        }
        return matchesState(status)
                && AgentRunMetadata.matches(status, tenantId, AgentRunMetadata.TENANT, AgentRunMetadata.TENANT_ID)
                && AgentRunMetadata.matches(status, sessionId, AgentRunMetadata.SESSION, AgentRunMetadata.SESSION_ID)
                && AgentRunMetadata.matches(status, surfaceId, AgentRunMetadata.SURFACE, AgentRunMetadata.SURFACE_ID)
                && AgentRunMetadata.matches(
                        status,
                        profileId,
                        AgentRunMetadata.PROFILE,
                        AgentRunMetadata.PROFILE_ID,
                        AgentRunMetadata.WAYANG_PROFILE);
    }

    private boolean matchesState(AgentRunStatus status) {
        return state == null || status.handle().state() == state;
    }

    private static AgentRunState parseState(String value) {
        return AgentRunStates.parseOptional(value);
    }
}
