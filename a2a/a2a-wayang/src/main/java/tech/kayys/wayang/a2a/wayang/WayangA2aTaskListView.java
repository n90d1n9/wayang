package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-specific task list envelopes backed by one task projection.
 */
record WayangA2aTaskListView(List<A2aTask> tasks, int pageSize) {

    WayangA2aTaskListView {
        tasks = tasks == null
                ? List.of()
                : tasks.stream()
                        .filter(Objects::nonNull)
                        .toList();
        pageSize = pageSize <= 0 ? tasks.size() : pageSize;
    }

    static WayangA2aTaskListView fromStore(WayangA2aTaskStore store, WayangA2aTaskQuery query) {
        WayangA2aTaskStore resolvedStore = Objects.requireNonNull(store, "store");
        WayangA2aTaskQuery resolvedQuery = query == null ? WayangA2aTaskQuery.all() : query;
        return new WayangA2aTaskListView(resolvedStore.list(resolvedQuery), resolvedQuery.limit());
    }

    Map<String, Object> toHttpMap() {
        List<Map<String, Object>> values = taskMaps();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskCount", values.size());
        payload.put("tasks", values);
        return WayangA2aMaps.copyMap(payload);
    }

    Map<String, Object> toJsonRpcMap() {
        List<Map<String, Object>> values = taskMaps();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tasks", values);
        payload.put("nextPageToken", "");
        payload.put("pageSize", pageSize);
        payload.put("totalSize", values.size());
        return WayangA2aMaps.copyMap(payload);
    }

    private List<Map<String, Object>> taskMaps() {
        return tasks.stream()
                .map(A2aTask::toMap)
                .toList();
    }
}
