package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpHistoryScopedReaderTest {

    @Test
    void filtersScopedSnapshotWithDomainFilterer() {
        McpHistoryScopedReader<HistoryItem, String> reader = reader();

        List<HistoryItem> result = reader.filteredEntries("scope-1", "keep")
                .await().indefinitely();

        assertEquals(List.of("b"), result.stream().map(HistoryItem::id).toList());
    }

    @Test
    void mapsFilteredEntries() {
        McpHistoryScopedReader<HistoryItem, String> reader = reader();

        int count = reader.mapFilteredEntries("scope-1", "keep", List::size)
                .await().indefinitely();

        assertEquals(1, count);
    }

    private static McpHistoryScopedReader<HistoryItem, String> reader() {
        return McpHistoryScopedReader.of(
                ignored -> Uni.createFrom().item(List.of(
                        new HistoryItem("a", "drop"),
                        new HistoryItem("b", "keep"))),
                (entries, group) -> entries.stream()
                        .filter(item -> item.group().equals(group))
                        .toList());
    }

    private record HistoryItem(
            String id,
            String group) {
    }
}
