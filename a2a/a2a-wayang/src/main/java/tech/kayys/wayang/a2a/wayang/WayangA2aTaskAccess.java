package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.Objects;
import java.util.Optional;

/**
 * Shared task visibility rules for tenant-scoped A2A requests.
 */
final class WayangA2aTaskAccess {

    private WayangA2aTaskAccess() {
    }

    static Optional<A2aTask> getForHttp(
            WayangA2aTaskStore store,
            WayangA2aHttpRequest request,
            String taskId) {
        return get(store, taskId, WayangA2aTenantHints.fromHttpRequest(request));
    }

    static Optional<A2aTask> getForJsonRpc(
            WayangA2aTaskStore store,
            WayangA2aJsonRpcRequest request,
            String taskId) {
        return get(store, taskId, WayangA2aTenantHints.fromMap(request.params()));
    }

    static Optional<A2aTask> get(
            WayangA2aTaskStore store,
            String taskId,
            Optional<String> tenant) {
        WayangA2aTaskStore resolvedStore = Objects.requireNonNull(store, "store");
        String resolvedTaskId = WayangA2aMaps.required(taskId, "taskId");
        Optional<String> resolvedTenant = tenant == null ? Optional.empty() : tenant;
        return resolvedStore.get(resolvedTaskId)
                .filter(task -> visibleToTenant(task, resolvedTenant));
    }

    static boolean visibleToTenant(A2aTask task, Optional<String> tenant) {
        if (task == null) {
            return false;
        }
        Optional<String> resolvedTenant = tenant == null ? Optional.empty() : tenant;
        return resolvedTenant.isEmpty()
                || resolvedTenant.get().equals(WayangA2aTenantHints.fromTask(task).orElse(null));
    }
}
