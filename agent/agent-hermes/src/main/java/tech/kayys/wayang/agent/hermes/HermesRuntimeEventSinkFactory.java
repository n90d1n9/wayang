package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Assembles the runtime event sink used by Hermes from an explicit sink bean
 * and the configured runtime journal backend.
 */
final class HermesRuntimeEventSinkFactory {

    private HermesRuntimeEventSinkFactory() {
    }

    static HermesRuntimeEventSink create(
            HermesAgentModeConfig config,
            Optional<HermesRuntimeEventSink> runtimeEventSink,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return create(
                config,
                runtimeEventSink,
                HermesPersistenceResources.of(objectStorageService, dataSource));
    }

    static HermesRuntimeEventSink create(
            HermesAgentModeConfig config,
            Optional<HermesRuntimeEventSink> runtimeEventSink,
            HermesPersistenceResources resources) {
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        return HermesRuntimeEventSinkResolver.compose(
                config,
                HermesOptionals.orEmpty(runtimeEventSink),
                effectiveResources.objectStorageService(),
                effectiveResources.dataSource());
    }
}
