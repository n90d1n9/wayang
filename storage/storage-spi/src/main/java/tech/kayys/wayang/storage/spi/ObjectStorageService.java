package tech.kayys.wayang.storage.spi;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Optional;

/**
 * Provider-neutral object storage contract for arbitrary artifact payloads.
 */
public interface ObjectStorageService {

    Uni<Optional<byte[]>> getObject(String key);

    Uni<List<String>> listObjects(String prefix);

    Uni<Void> putObject(String key, byte[] data);

    Uni<Boolean> deleteObject(String key);
}
