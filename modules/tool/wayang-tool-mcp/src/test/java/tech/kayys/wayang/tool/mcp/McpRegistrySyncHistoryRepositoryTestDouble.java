package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class McpRegistrySyncHistoryRepositoryTestDouble implements RegistrySyncHistoryRepository {
    private final List<RegistrySyncHistory> items;
    private int listByRequestIdCalls;
    private String lastRequestId;
    private int lastLimit;

    McpRegistrySyncHistoryRepositoryTestDouble() {
        this(List.of());
    }

    McpRegistrySyncHistoryRepositoryTestDouble(List<RegistrySyncHistory> items) {
        this.items = new ArrayList<>(items);
    }

    List<RegistrySyncHistory> items() {
        return items;
    }

    void add(RegistrySyncHistory history) {
        items.add(history);
    }

    void addAll(List<RegistrySyncHistory> history) {
        items.addAll(history);
    }

    int listByRequestIdCalls() {
        return listByRequestIdCalls;
    }

    String lastRequestId() {
        return lastRequestId;
    }

    int lastLimit() {
        return lastLimit;
    }

    @Override
    public Uni<RegistrySyncHistory> save(RegistrySyncHistory history) {
        items.add(history);
        return Uni.createFrom().item(history);
    }

    @Override
    public Uni<List<RegistrySyncHistory>> listByRequestId(String requestId, int limit) {
        listByRequestIdCalls++;
        lastRequestId = requestId;
        lastLimit = limit;
        return Uni.createFrom().item(items.stream()
                .filter(item -> Objects.equals(requestId, item.getRequestId()))
                .limit(Math.max(1, limit))
                .toList());
    }
}
