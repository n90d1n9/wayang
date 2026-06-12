package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class InMemoryPromotionReceiptDataSource extends AbstractHermesJdbcDataSource {

    private final List<PromotionReceiptRow> rows = new ArrayList<>();
    private long sequence;

    @Override
    protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
        String normalizedSql = sql.toUpperCase(Locale.ROOT);
        if (normalizedSql.startsWith("INSERT")) {
            rows.add(new PromotionReceiptRow(
                    ++sequence,
                    (String) parameters.get(1),
                    (String) parameters.get(2),
                    (String) parameters.get(3),
                    (String) parameters.get(4),
                    (String) parameters.get(5),
                    (String) parameters.get(6),
                    (String) parameters.get(7),
                    (String) parameters.get(8)));
            return 1;
        }
        if (normalizedSql.startsWith("DELETE")) {
            String recordId = (String) parameters.get(1);
            int before = rows.size();
            rows.removeIf(row -> row.recordId().equals(recordId));
            return before - rows.size();
        }
        return 0;
    }

    @Override
    protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
        String normalizedSql = sql.toUpperCase(Locale.ROOT);
        if (normalizedSql.contains("COUNT(*)")) {
            return List.of(List.of(rows.size()));
        }
        if (normalizedSql.contains("WHERE IDEMPOTENCY_KEY = ?")) {
            String idempotencyKey = (String) parameters.get(1);
            return rows.stream()
                    .filter(row -> row.idempotencyKey().equals(idempotencyKey))
                    .sorted(rowOrdering())
                    .map(row -> List.<Object>of(row.recordMetadata()))
                    .toList();
        }
        if (normalizedSql.startsWith("SELECT RECORD_METADATA")) {
            return rows.stream()
                    .sorted(rowOrdering())
                    .map(row -> List.<Object>of(row.recordMetadata()))
                    .toList();
        }
        if (normalizedSql.startsWith("SELECT RECORD_ID")) {
            return rows.stream()
                    .sorted(rowOrdering())
                    .map(row -> List.<Object>of(row.recordId()))
                    .toList();
        }
        return List.of();
    }

    private Comparator<PromotionReceiptRow> rowOrdering() {
        return Comparator
                .comparing(PromotionReceiptRow::recordedAt)
                .thenComparing(PromotionReceiptRow::sequence)
                .reversed();
    }

    private record PromotionReceiptRow(
            long sequence,
            String recordId,
            String idempotencyKey,
            String promotionId,
            String skillId,
            String status,
            String outcome,
            String recordMetadata,
            String recordedAt) {
    }
}
