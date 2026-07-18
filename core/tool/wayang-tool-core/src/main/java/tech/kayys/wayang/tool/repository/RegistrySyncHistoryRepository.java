package tech.kayys.wayang.tool.repository;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.util.List;

public interface RegistrySyncHistoryRepository {

    Uni<RegistrySyncHistory> save(RegistrySyncHistory history);

    Uni<List<RegistrySyncHistory>> listByRequestId(String requestId, int limit);
}

