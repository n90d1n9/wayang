package tech.kayys.wayang.rag.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolver;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

@ApplicationScoped
public class RagPluginConfigStartupValidator {

        @Inject
        RagRuntimeConfig config;
        @Inject
        RagPluginTenantStrategyResolver strategyResolver;

        @PostConstruct
        void validateOnStartup() {
                validateNow();
        }

        void validateNow() {
                String strategy = config.getRagPluginSelectionStrategy();
                if (strategy == null || strategy.isBlank()) {
                        throw new IllegalStateException("Invalid RAG plugin configuration [empty_strategy_id] "
                                        + "field=`rag.runtime.rag.plugins.selection-strategy` value=`"
                                        + (strategy == null ? "" : strategy) + "`: Strategy id must be non-empty.");
                }
                if (!strategyResolver.isKnownStrategy(strategy)) {
                        throw new IllegalStateException("Invalid RAG plugin configuration [unknown_strategy_id] "
                                        + "field=`rag.runtime.rag.plugins.selection-strategy` value=`" + strategy
                                        + "`: Unknown strategy id.");
                }

                RagPluginConfigValidation.validateTenantOverrideSyntax(
                                "rag.runtime.rag.plugins.tenant-enabled",
                                config.getRagPluginTenantEnabledOverrides(),
                                true,
                                (code, field, tenant, value, detail) -> new IllegalStateException(
                                                "Invalid RAG plugin configuration [" + code + "] field=`" + field
                                                                + "` tenant=`" + (tenant == null ? "" : tenant)
                                                                + "` value=`" + (value == null ? "" : value) + "`: "
                                                                + detail));
                RagPluginConfigValidation.validateTenantOverrideSyntax(
                                "rag.runtime.rag.plugins.tenant-order",
                                config.getRagPluginTenantOrderOverrides(),
                                false,
                                (code, field, tenant, value, detail) -> new IllegalStateException(
                                                "Invalid RAG plugin configuration [" + code + "] field=`" + field
                                                                + "` tenant=`" + (tenant == null ? "" : tenant)
                                                                + "` value=`" + (value == null ? "" : value) + "`: "
                                                                + detail));
        }
}
