package tech.kayys.wayang.rag.config;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolver;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RagPluginConfigStartupValidatorTest {

    @Test
    void shouldAllowValidStartupConfig() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginTenantEnabledOverrides("tenant-a=normalize-query,safety-filter;tenant-b=*");
        config.setRagPluginTenantOrderOverrides("tenant-a=safety-filter,normalize-query");

        RagPluginConfigStartupValidator validator = new RagPluginConfigStartupValidator();
        validator.config = config;
        validator.strategyResolver = new RagPluginTenantStrategyResolver(config);

        assertDoesNotThrow(validator::validateNow);
    }

    @Test
    void shouldFailStartupOnInvalidTenantEnabledOverrides() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginTenantEnabledOverrides("tenant-a");

        RagPluginConfigStartupValidator validator = new RagPluginConfigStartupValidator();
        validator.config = config;
        validator.strategyResolver = new RagPluginTenantStrategyResolver(config);

        assertThrows(IllegalStateException.class, validator::validateNow);
    }

    @Test
    void shouldFailStartupOnInvalidTenantOrderOverrides() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginTenantOrderOverrides("tenant-a=*");

        RagPluginConfigStartupValidator validator = new RagPluginConfigStartupValidator();
        validator.config = config;
        validator.strategyResolver = new RagPluginTenantStrategyResolver(config);

        assertThrows(IllegalStateException.class, validator::validateNow);
    }

    @Test
    void shouldFailStartupOnUnknownSelectionStrategy() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setRagPluginSelectionStrategy("unknown");

        RagPluginConfigStartupValidator validator = new RagPluginConfigStartupValidator();
        validator.config = config;
        validator.strategyResolver = new RagPluginTenantStrategyResolver(config);

        assertThrows(IllegalStateException.class, validator::validateNow);
    }
}
