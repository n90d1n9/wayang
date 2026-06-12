package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Hermes learning promotion receipt storage from persistence hints.
 */
public final class HermesLearningPromotionReceiptLedgerResolver {

    public static final String DEFAULT_FILE_SYSTEM_PATH =
            HermesLearningPromotionReceiptLedgerSettings.DEFAULT_FILE_SYSTEM_PATH;

    private HermesLearningPromotionReceiptLedgerResolver() {
    }

    public static HermesLearningPromotionReceiptLedger resolve(HermesAgentModeConfig config) {
        return resolve(config, Optional.empty());
    }

    public static HermesLearningPromotionReceiptLedger resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService) {
        return resolve(config, objectStorageService, Optional.empty());
    }

    public static HermesLearningPromotionReceiptLedger resolve(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.from(config);
        HermesPersistenceResources resources = HermesPersistenceResources.of(objectStorageService, dataSource);
        return HermesLearningPromotionReceiptLedgerFactory.create(settings, resources);
    }

    public static Map<String, Object> metadata(HermesAgentModeConfig config) {
        return HermesLearningPromotionReceiptLedgerSettings.from(config).toMetadata();
    }
}
