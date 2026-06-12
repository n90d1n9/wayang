package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagPluginAdminStatusTest {

    @Test
    void normalizesTenantAndCopiesListsDefensively() {
        List<RagPluginInspection> plugins = new ArrayList<>();
        plugins.add(new RagPluginInspection("a", 10, true, true, true));
        List<String> activePluginIds = new ArrayList<>();
        activePluginIds.add("a");
        Instant observedAt = Instant.parse("2026-05-27T00:00:00Z");

        RagPluginAdminStatus status = new RagPluginAdminStatus(
                " tenant ",
                null,
                plugins,
                activePluginIds,
                observedAt);

        plugins.add(new RagPluginInspection("b", 20, true, true, false));
        activePluginIds.add("b");

        assertEquals("tenant", status.tenantId());
        assertEquals(List.of(new RagPluginInspection("a", 10, true, true, true)), status.plugins());
        assertEquals(List.of("a"), status.activePluginIds());
        assertEquals(observedAt, status.observedAt());
        assertThrows(UnsupportedOperationException.class,
                () -> status.activePluginIds().add("other"));
    }

    @Test
    void defaultsNullListsAndObservedAt() {
        RagPluginAdminStatus status = new RagPluginAdminStatus(null, null, null, null, null);

        assertEquals("", status.tenantId());
        assertEquals(List.of(), status.plugins());
        assertEquals(List.of(), status.activePluginIds());
        assertNotNull(status.observedAt());
    }
}
