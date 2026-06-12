package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/**
 * Assembles the Hermes learning loop with its persistence-side collaborators.
 */
final class HermesLearningLoopFactory {

    private HermesLearningLoopFactory() {
    }

    static HermesLearningLoop create(
            HermesLearnedSkillRepository learnedSkills,
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return create(
                learnedSkills,
                new HermesSkillDistiller(),
                config,
                new HermesLearningSignalFactory(),
                new HermesSkillReusePolicy(),
                HermesPersistenceResources.of(objectStorageService, dataSource));
    }

    static HermesLearningLoop create(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy,
            HermesPersistenceResources resources) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        HermesLearningPromotionReceiptLedger receiptLedger = receiptLedger(effectiveConfig, effectiveResources);
        return create(
                learnedSkills,
                distiller,
                effectiveConfig,
                signalFactory,
                reusePolicy,
                receiptLedger);
    }

    static HermesLearningLoop create(
            HermesLearnedSkillRepository learnedSkills,
            HermesSkillDistiller distiller,
            HermesAgentModeConfig config,
            HermesLearningSignalFactory signalFactory,
            HermesSkillReusePolicy reusePolicy,
            HermesLearningPromotionReceiptLedger receiptLedger) {
        return new HermesLearningLoop(
                Objects.requireNonNull(learnedSkills, "learnedSkills"),
                distiller,
                config,
                signalFactory,
                reusePolicy,
                receiptLedger);
    }

    static HermesLearningPromotionReceiptLedger receiptLedger(
            HermesAgentModeConfig config,
            HermesPersistenceResources resources) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesPersistenceResources effectiveResources = resources == null
                ? HermesPersistenceResources.empty()
                : resources;
        return HermesLearningPromotionReceiptLedgerResolver.resolve(
                effectiveConfig,
                effectiveResources.objectStorageService(),
                effectiveResources.dataSource());
    }
}
