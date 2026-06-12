package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Aggregate read model for a bounded Hermes learning promotion audit window.
 */
public record HermesLearningAuditSummary(
        int scannedReceipts,
        int matchedReceipts,
        boolean truncated,
        long persistedReceipts,
        long skippedReceipts,
        long rejectedReceipts,
        long approvedReceipts,
        long distinctSkills,
        String latestRecordedAt,
        String latestPromotionId,
        String latestSkillId,
        String latestOutcome,
        Map<String, Long> statusCounts,
        Map<String, Long> outcomeCounts,
        Map<String, Long> adapterCounts) {

    public HermesLearningAuditSummary {
        scannedReceipts = Math.max(scannedReceipts, 0);
        matchedReceipts = Math.max(matchedReceipts, scannedReceipts);
        latestRecordedAt = HermesText.oneLineOr(latestRecordedAt, "");
        latestPromotionId = HermesText.trimToEmpty(latestPromotionId);
        latestSkillId = HermesText.trimToEmpty(latestSkillId);
        latestOutcome = HermesText.oneLineOr(latestOutcome, "");
        statusCounts = copy(statusCounts);
        outcomeCounts = copy(outcomeCounts);
        adapterCounts = copy(adapterCounts);
    }

    public static HermesLearningAuditSummary empty() {
        return from(HermesLearningPromotionReceiptPage.empty(HermesLearningPromotionReceiptQuery.recent()));
    }

    public static HermesLearningAuditSummary from(HermesLearningPromotionReceiptPage page) {
        HermesLearningPromotionReceiptPage resolved = page == null
                ? HermesLearningPromotionReceiptPage.empty(HermesLearningPromotionReceiptQuery.recent())
                : page;
        List<HermesLearningPromotionReceiptLedgerEntry> entries = resolved.entries();
        HermesLearningPromotionReceiptLedgerEntry latest = entries.isEmpty() ? null : entries.get(0);
        HermesLearningPromotionReceipt latestReceipt = latest == null ? null : latest.receipt();
        return new HermesLearningAuditSummary(
                entries.size(),
                resolved.matchedReceipts(),
                resolved.truncated(),
                entries.stream().map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .filter(HermesLearningPromotionReceipt::persisted)
                        .count(),
                entries.stream().map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .filter(receipt -> HermesLearningPromotionReceipt.OUTCOME_SKIPPED.equals(receipt.outcome()))
                        .count(),
                entries.stream().map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .filter(receipt -> HermesLearningPromotionReceipt.OUTCOME_REJECTED.equals(receipt.outcome()))
                        .count(),
                entries.stream().map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .filter(receipt -> HermesLearningPromotion.STATUS_APPROVED.equals(receipt.status()))
                        .count(),
                entries.stream().map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .map(HermesLearningPromotionReceipt::skillId)
                        .filter(value -> !HermesText.trimToEmpty(value).isBlank())
                        .distinct()
                        .count(),
                latest == null ? "" : latest.recordedAt(),
                latestReceipt == null ? "" : latestReceipt.promotionId(),
                latestReceipt == null ? "" : latestReceipt.skillId(),
                latestReceipt == null ? "" : latestReceipt.outcome(),
                counts(entries, HermesLearningPromotionReceipt::status, true),
                counts(entries, HermesLearningPromotionReceipt::outcome, true),
                counts(entries, HermesLearningPromotionReceipt::adapterId, false));
    }

    public boolean hasReceipts() {
        return scannedReceipts > 0;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("scannedReceipts", scannedReceipts);
        values.put("matchedReceipts", matchedReceipts);
        values.put("truncated", truncated);
        values.put("persistedReceipts", persistedReceipts);
        values.put("skippedReceipts", skippedReceipts);
        values.put("rejectedReceipts", rejectedReceipts);
        values.put("approvedReceipts", approvedReceipts);
        values.put("distinctSkills", distinctSkills);
        values.put("latestRecordedAt", latestRecordedAt);
        values.put("latestPromotionId", latestPromotionId);
        values.put("latestSkillId", latestSkillId);
        values.put("latestOutcome", latestOutcome);
        values.put("statusCounts", statusCounts);
        values.put("outcomeCounts", outcomeCounts);
        values.put("adapterCounts", adapterCounts);
        return Map.copyOf(values);
    }

    private static Map<String, Long> counts(
            List<HermesLearningPromotionReceiptLedgerEntry> entries,
            Function<HermesLearningPromotionReceipt, String> classifier,
            boolean includeBlank) {
        Map<String, Long> counts = new LinkedHashMap<>();
        if (entries == null) {
            return counts;
        }
        for (HermesLearningPromotionReceiptLedgerEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = HermesText.trimToEmpty(classifier.apply(entry.receipt()));
            if (value.isBlank() && !includeBlank) {
                continue;
            }
            counts.merge(value.isBlank() ? "unknown" : value, 1L, Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> copy(Map<String, Long> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
