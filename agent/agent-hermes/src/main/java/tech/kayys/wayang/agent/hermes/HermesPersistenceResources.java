package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Optional persistence adapters available to Hermes store resolvers.
 */
record HermesPersistenceResources(
        Optional<ObjectStorageService> objectStorageService,
        Optional<DataSource> dataSource) {

    HermesPersistenceResources {
        objectStorageService = objectStorageService == null ? Optional.empty() : objectStorageService;
        dataSource = dataSource == null ? Optional.empty() : dataSource;
    }

    static HermesPersistenceResources empty() {
        return new HermesPersistenceResources(Optional.empty(), Optional.empty());
    }

    static HermesPersistenceResources of(
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return new HermesPersistenceResources(objectStorageService, dataSource);
    }

    ObjectStorageService requireObjectStorage(String message) {
        return objectStorageService.orElseThrow(() -> new IllegalStateException(message));
    }

    DataSource requireDataSource(String message) {
        return dataSource.orElseThrow(() -> new IllegalStateException(message));
    }

    <T> Optional<T> databaseThenObjectStorage(
            Function<DataSource, T> databaseFactory,
            Function<ObjectStorageService, T> objectStorageFactory) {
        return dataSource.map(databaseFactory)
                .or(() -> objectStorageService.map(objectStorageFactory));
    }

    <T> T databaseThenObjectStorageOr(
            Function<DataSource, T> databaseFactory,
            Function<ObjectStorageService, T> objectStorageFactory,
            Supplier<T> fallbackFactory) {
        return databaseThenObjectStorage(databaseFactory, objectStorageFactory)
                .orElseGet(fallbackFactory);
    }
}
