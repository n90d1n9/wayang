package tech.kayys.wayang.tenant;

import io.smallrye.mutiny.Uni;

public interface LocalTenantLookup {
    Uni<TenantContext> resolve(String apiKey);
}
