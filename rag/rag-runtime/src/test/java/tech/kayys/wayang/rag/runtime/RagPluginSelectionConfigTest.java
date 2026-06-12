package tech.kayys.wayang.rag.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginSelectionConfigTest {

    @Test
    void parsesTenantOverridesWithTrimmedTenantKeysAndImmutableResult() {
        Map<String, String> parsed = RagPluginSelectionConfig.parseTenantOverrides(
                " tenant-a = plugin-a,plugin-b ; ; bad ; tenant-b= * ; tenant-c=");

        assertEquals(2, parsed.size());
        assertEquals("plugin-a,plugin-b", parsed.get("tenant-a"));
        assertEquals("*", parsed.get("tenant-b"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.put("other", "plugin"));
    }

    @Test
    void parsesEnabledPluginIdsWithWildcardDefaultAndNormalizedIds() {
        assertEquals(Set.of(RagPluginSelectionConfig.WILDCARD_PLUGIN_ID),
                RagPluginSelectionConfig.parseEnabledPluginIds(null));

        Set<String> parsed = RagPluginSelectionConfig.parseEnabledPluginIds(" Plugin-A,plugin-b\nPLUGIN-A ");

        assertEquals(Set.of("plugin-a", "plugin-b"), parsed);
        assertTrue(RagPluginSelectionConfig.allPluginsEnabled(Set.of(
                RagPluginSelectionConfig.WILDCARD_PLUGIN_ID)));
    }

    @Test
    void parsesPluginOrderAsStableNormalizedList() {
        List<String> parsed = RagPluginSelectionConfig.parsePluginOrder(" Plugin-B, plugin-a\nplugin-c ");

        assertEquals(List.of("plugin-b", "plugin-a", "plugin-c"), parsed);
    }

    @Test
    void ignoresBlankPluginTokensAcrossLists() {
        assertEquals(
                List.of("plugin-a", "plugin-b"),
                RagPluginSelectionConfig.parsePluginOrder(" , Plugin-A,\n,, plugin-b, "));
        assertEquals(
                Set.of("plugin-a", "plugin-b"),
                RagPluginSelectionConfig.parseEnabledPluginIds(" , Plugin-A,\n,, plugin-b, "));
    }

    @Test
    void normalizesTenantAndStrategyIds() {
        assertEquals("tenant-a", RagPluginSelectionConfig.normalizeTenant(" tenant-a "));
        assertEquals("", RagPluginSelectionConfig.normalizeTenant(null));
        assertEquals("config", RagPluginSelectionConfig.normalizeStrategyId(" Config "));
        assertEquals("", RagPluginSelectionConfig.normalizeStrategyId(null));
    }
}
