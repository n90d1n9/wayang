package tech.kayys.wayang.tenant.cas;

import java.util.Set;

public record CasConsumerContext(
        boolean active,
        String consumerId,
        String tenantId,
        String workspaceId,
        String planId,
        Set<String> scopes
) {
}
