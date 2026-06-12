package tech.kayys.wayang.agent.hermes;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded query result for learned-skill promotion receipt audit views.
 */
public record HermesLearningPromotionReceiptPage(
        HermesLearningPromotionReceiptQuery query,
        List<HermesLearningPromotionReceiptLedgerEntry> entries,
        int matchedReceipts,
        int totalMatchedReceipts,
        String previousCursor,
        String nextCursor,
        String firstCursor,
        String lastCursor,
        boolean hasPreviousPage,
        boolean hasNextPage,
        boolean cursorResolved) {

    private static final Comparator<HermesLearningPromotionReceiptLedgerEntry> RECENT_FIRST =
            Comparator.comparing(HermesLearningPromotionReceiptLedgerEntry::recordedAt)
                    .reversed()
                    .thenComparing(entry -> entry.receipt().idempotencyKey());

    public HermesLearningPromotionReceiptPage(
            HermesLearningPromotionReceiptQuery query,
            List<HermesLearningPromotionReceiptLedgerEntry> entries,
            int matchedReceipts) {
        this(query, entries, matchedReceipts, matchedReceipts, "", "", "", "", false, false, true);
    }

    public HermesLearningPromotionReceiptPage {
        query = query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        entries = HermesCollections.copyNonNull(entries);
        matchedReceipts = Math.max(matchedReceipts, entries.size());
        totalMatchedReceipts = Math.max(totalMatchedReceipts, matchedReceipts);
        previousCursor = HermesText.trimToEmpty(previousCursor);
        nextCursor = HermesText.trimToEmpty(nextCursor);
        firstCursor = HermesText.trimToEmpty(firstCursor);
        lastCursor = HermesText.trimToEmpty(lastCursor);
    }

    public static HermesLearningPromotionReceiptPage empty(
            HermesLearningPromotionReceiptQuery query) {
        return new HermesLearningPromotionReceiptPage(query, List.of(), 0);
    }

    public static HermesLearningPromotionReceiptPage fromEntries(
            Collection<HermesLearningPromotionReceiptLedgerEntry> entries,
            HermesLearningPromotionReceiptQuery query) {
        HermesLearningPromotionReceiptQuery resolved =
                query == null ? HermesLearningPromotionReceiptQuery.recent() : query;
        List<HermesLearningPromotionReceiptLedgerEntry> matched =
                (entries == null ? List.<HermesLearningPromotionReceiptLedgerEntry>of() : entries).stream()
                        .filter(entry -> entry != null && entry.matches(resolved))
                        .sorted(RECENT_FIRST)
                        .toList();
        return page(matched, resolved);
    }

    public List<HermesLearningPromotionReceipt> receipts() {
        return entries.stream()
                .map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                .toList();
    }

    public int returnedReceipts() {
        return entries.size();
    }

    public boolean truncated() {
        return matchedReceipts > returnedReceipts();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("query", query.toMetadata());
        values.put("matchedReceipts", matchedReceipts);
        values.put("totalMatchedReceipts", totalMatchedReceipts);
        values.put("returnedReceipts", returnedReceipts());
        values.put("truncated", truncated());
        values.put("previousCursor", previousCursor);
        values.put("nextCursor", nextCursor);
        values.put("firstCursor", firstCursor);
        values.put("lastCursor", lastCursor);
        values.put("hasPreviousPage", hasPreviousPage);
        values.put("hasNextPage", hasNextPage);
        values.put("cursorResolved", cursorResolved);
        values.put("entries", entries.stream()
                .map(HermesLearningPromotionReceiptLedgerEntry::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static HermesLearningPromotionReceiptPage page(
            List<HermesLearningPromotionReceiptLedgerEntry> matched,
            HermesLearningPromotionReceiptQuery query) {
        String beforeReceiptId = query.beforeReceiptId();
        String afterReceiptId = query.afterReceiptId();
        if (!afterReceiptId.isBlank()) {
            int index = indexOf(matched, afterReceiptId);
            if (index < 0) {
                return page(query, List.of(), 0, matched.size(), false, false, false);
            }
            int fromIndex = index + 1;
            return pageFromRange(query, matched, matched.subList(fromIndex, matched.size()), fromIndex, false);
        }
        if (!beforeReceiptId.isBlank()) {
            int index = indexOf(matched, beforeReceiptId);
            if (index < 0) {
                return page(query, List.of(), 0, matched.size(), false, false, false);
            }
            return pageFromRange(query, matched, matched.subList(0, index), 0, true);
        }
        return pageFromRange(query, matched, matched, 0, false);
    }

    private static HermesLearningPromotionReceiptPage pageFromRange(
            HermesLearningPromotionReceiptQuery query,
            List<HermesLearningPromotionReceiptLedgerEntry> allMatched,
            List<HermesLearningPromotionReceiptLedgerEntry> candidates,
            int candidateOffset,
            boolean closestToCursor) {
        int safeLimit = Math.max(query.limit(), 0);
        int relativeFrom = closestToCursor
                ? Math.max(candidates.size() - safeLimit, 0)
                : 0;
        int relativeTo = closestToCursor
                ? candidates.size()
                : Math.min(safeLimit, candidates.size());
        List<HermesLearningPromotionReceiptLedgerEntry> returned =
                List.copyOf(candidates.subList(relativeFrom, relativeTo));
        int absoluteFrom = candidateOffset + relativeFrom;
        int absoluteTo = candidateOffset + relativeTo;
        boolean hasPrevious = absoluteFrom > 0;
        boolean hasNext = absoluteTo < allMatched.size();
        return page(query, returned, candidates.size(), allMatched.size(), hasPrevious, hasNext, true);
    }

    private static HermesLearningPromotionReceiptPage page(
            HermesLearningPromotionReceiptQuery query,
            List<HermesLearningPromotionReceiptLedgerEntry> returned,
            int matchedReceipts,
            int totalMatchedReceipts,
            boolean hasPrevious,
            boolean hasNext,
            boolean cursorResolved) {
        String firstCursor = cursor(returned, 0);
        String lastCursor = cursor(returned, returned.size() - 1);
        return new HermesLearningPromotionReceiptPage(
                query,
                returned,
                matchedReceipts,
                totalMatchedReceipts,
                hasPrevious ? firstCursor : "",
                hasNext ? lastCursor : "",
                firstCursor,
                lastCursor,
                hasPrevious,
                hasNext,
                cursorResolved);
    }

    private static int indexOf(
            List<HermesLearningPromotionReceiptLedgerEntry> entries,
            String receiptId) {
        String resolvedReceiptId = HermesLearningPromotionReceiptLedgerRecords.key(receiptId);
        if (resolvedReceiptId.isBlank()) {
            return -1;
        }
        for (int index = 0; index < entries.size(); index++) {
            if (resolvedReceiptId.equals(entries.get(index).receipt().idempotencyKey())) {
                return index;
            }
        }
        return -1;
    }

    private static String cursor(List<HermesLearningPromotionReceiptLedgerEntry> entries, int index) {
        if (entries == null || index < 0 || index >= entries.size()) {
            return "";
        }
        return entries.get(index).receipt().idempotencyKey();
    }
}
