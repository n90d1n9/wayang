package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemoryHermesObjectStorageService implements ObjectStorageService {

    final Map<String, byte[]> objects = new LinkedHashMap<>();

    @Override
    public Uni<Optional<byte[]>> getObject(String key) {
        return Uni.createFrom().item(Optional.ofNullable(objects.get(key)));
    }

    @Override
    public Uni<List<String>> listObjects(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix;
        return Uni.createFrom().item(objects.keySet().stream()
                .filter(key -> key.startsWith(normalizedPrefix))
                .toList());
    }

    @Override
    public Uni<Void> putObject(String key, byte[] data) {
        objects.put(key, data == null ? new byte[0] : data);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Boolean> deleteObject(String key) {
        return Uni.createFrom().item(objects.remove(key) != null);
    }
}
