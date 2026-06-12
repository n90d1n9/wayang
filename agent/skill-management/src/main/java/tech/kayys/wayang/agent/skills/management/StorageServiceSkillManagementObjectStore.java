package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bridge from the platform storage SPI to the skill-management object store.
 */
public class StorageServiceSkillManagementObjectStore implements SkillManagementObjectStore {

    private final ObjectStorageService storageService;

    public StorageServiceSkillManagementObjectStore(ObjectStorageService storageService) {
        this.storageService = Objects.requireNonNull(storageService, "storageService");
    }

    @Override
    public Optional<byte[]> get(String key) {
        return storageService.getObject(key).await().indefinitely();
    }

    @Override
    public List<String> list(String prefix) {
        return storageService.listObjects(prefix).await().indefinitely();
    }

    @Override
    public void put(String key, byte[] content) {
        storageService.putObject(key, content).await().indefinitely();
    }

    @Override
    public boolean delete(String key) {
        return storageService.deleteObject(key).await().indefinitely();
    }
}
