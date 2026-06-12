package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;

import java.util.Map;

/**
 * Coordinates optional learned-skill indexing and attaches indexing lifecycle metadata.
 */
public final class HermesLearningIndexingCoordinator {

    private final HermesLearnedSkillIndexer indexer;

    public HermesLearningIndexingCoordinator(HermesLearnedSkillIndexer indexer) {
        this.indexer = indexer == null ? HermesLearnedSkillIndexer.noop() : indexer;
    }

    public Uni<HermesLearningResult> index(
            HermesLearningResult result,
            HermesLearningPromotion promotion,
            HermesLearningSignal signal) {
        HermesLearningResult resolved = result == null
                ? HermesLearningResult.skipped("learning result missing")
                : result;
        HermesLearningIndexingRequest request = new HermesLearningIndexingRequest(
                promotion,
                resolved,
                signal);
        return indexer.index(request)
                .onItem().ifNull().continueWith(() -> HermesLearningIndexingReceipt.skipped(
                        request,
                        "indexer returned no receipt",
                        adapterId()))
                .onFailure().recoverWithItem(error -> HermesLearningIndexingReceipt.failed(
                        request,
                        error,
                        adapterId()))
                .map(receipt -> attachReceipt(resolved, receipt));
    }

    private HermesLearningResult attachReceipt(
            HermesLearningResult result,
            HermesLearningIndexingReceipt receipt) {
        HermesLearningLifecycleReport lifecycle = result.metadataView()
                .lifecycleReport()
                .withStage(stage(receipt));
        return result.withSkillIndexingReceipt(receipt)
                .withLifecycleReport(lifecycle);
    }

    private HermesLearningStageReport stage(HermesLearningIndexingReceipt receipt) {
        Map<String, Object> metadata = receipt.toMetadata();
        if (receipt.indexed()) {
            return HermesLearningStageReport.completed(
                    HermesLearningStageCatalog.SKILL_INDEXING,
                    receipt.reason(),
                    metadata);
        }
        if (HermesLearningIndexingReceipt.STATUS_FAILED.equals(receipt.status())) {
            return HermesLearningStageReport.failed(
                    HermesLearningStageCatalog.SKILL_INDEXING,
                    receipt.reason(),
                    metadata);
        }
        return HermesLearningStageReport.skipped(
                HermesLearningStageCatalog.SKILL_INDEXING,
                receipt.reason()).withMetadata(metadata);
    }

    private String adapterId() {
        return HermesText.trimOr(indexer.adapterId(), indexer.getClass().getSimpleName());
    }
}
