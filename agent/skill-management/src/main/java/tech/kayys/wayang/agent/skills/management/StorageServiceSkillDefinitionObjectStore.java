package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

/**
 * Compatibility bridge from the platform storage SPI to the legacy definition
 * object-store name.
 *
 * @deprecated use {@link StorageServiceSkillManagementObjectStore}.
 */
@Deprecated(forRemoval = false)
public final class StorageServiceSkillDefinitionObjectStore
        extends StorageServiceSkillManagementObjectStore
        implements SkillDefinitionObjectStore {

    public StorageServiceSkillDefinitionObjectStore(ObjectStorageService storageService) {
        super(storageService);
    }
}
