package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Map;

/**
 * Builds learning promotion receipt ledger backends from effective settings.
 */
final class HermesLearningPromotionReceiptLedgerFactory {

    private HermesLearningPromotionReceiptLedgerFactory() {
    }

    static HermesLearningPromotionReceiptLedger create(
            HermesLearningPromotionReceiptLedgerSettings settings,
            HermesPersistenceResources resources) {
        HermesLearningPromotionReceiptLedgerSettings effectiveSettings = settings == null
                ? HermesLearningPromotionReceiptLedgerSettings.fromHints(Map.of())
                : settings;
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        return switch (effectiveSettings.store()) {
            case IN_MEMORY -> HermesLearningPromotionReceiptLedger.inMemory();
            case FILE_SYSTEM -> fileSystemLedger(effectiveSettings);
            case OBJECT_STORAGE -> objectStorageLedger(
                    effectiveSettings,
                    effectiveResources.requireObjectStorage(
                            "Object-storage Hermes learning promotion receipt ledger requires an ObjectStorageService"));
            case DATABASE -> databaseLedger(
                    effectiveSettings,
                    effectiveResources.requireDataSource(
                            "Database Hermes learning promotion receipt ledger requires a DataSource"));
            case HYBRID -> HermesLearningPromotionReceiptLedger.hybrid(
                    effectiveResources.databaseThenObjectStorageOr(
                            data -> (HermesLearningPromotionReceiptLedger) databaseLedger(effectiveSettings, data),
                            storage -> objectStorageLedger(effectiveSettings, storage),
                            HermesLearningPromotionReceiptLedger::inMemory),
                    fileSystemLedger(effectiveSettings));
            case NOOP -> HermesLearningPromotionReceiptLedger.noop();
        };
    }

    private static FileSystemHermesLearningPromotionReceiptLedger fileSystemLedger(
            HermesLearningPromotionReceiptLedgerSettings settings) {
        return new FileSystemHermesLearningPromotionReceiptLedger(
                Path.of(settings.path()),
                settings.maxRecords());
    }

    private static DatabaseHermesLearningPromotionReceiptLedger databaseLedger(
            HermesLearningPromotionReceiptLedgerSettings settings,
            DataSource dataSource) {
        return new DatabaseHermesLearningPromotionReceiptLedger(
                dataSource,
                settings.jdbcTableName(),
                settings.jdbcInitializeSchema(),
                settings.maxRecords());
    }

    private static ObjectStorageHermesLearningPromotionReceiptLedger objectStorageLedger(
            HermesLearningPromotionReceiptLedgerSettings settings,
            ObjectStorageService objectStorageService) {
        return new ObjectStorageHermesLearningPromotionReceiptLedger(
                objectStorageService,
                settings.objectPrefix(),
                settings.maxRecords());
    }
}
