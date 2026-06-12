package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter-facing journal read model that bundles query, page, and summary.
 */
public record HermesRuntimeJournalView(
        HermesRuntimeEventQuery query,
        HermesRuntimeEventPage page,
        HermesRuntimeJournalSummary summary) {

    public HermesRuntimeJournalView {
        query = query == null ? HermesRuntimeEventQuery.latest() : query;
        page = page == null ? new HermesRuntimeEventPage(List.of(), 0) : page;
        summary = summary == null ? HermesRuntimeJournalSummary.from(page) : summary;
    }

    public static HermesRuntimeJournalView from(
            HermesRuntimeEventQuery query,
            HermesRuntimeEventPage page) {
        HermesRuntimeEventPage resolvedPage = page == null
                ? new HermesRuntimeEventPage(List.of(), 0)
                : page;
        return new HermesRuntimeJournalView(
                query,
                resolvedPage,
                HermesRuntimeJournalSummary.from(resolvedPage));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("query", query.toMetadata());
        metadata.put("page", page.toMetadata());
        metadata.put("summary", summary.toMetadata());
        metadata.put("matchedEvents", page.matchedEvents());
        metadata.put("totalMatchedEvents", page.totalMatchedEvents());
        metadata.put("returnedEvents", page.returnedEvents());
        metadata.put("truncated", page.truncated());
        metadata.put("previousCursor", page.previousCursor());
        metadata.put("nextCursor", page.nextCursor());
        metadata.put("firstCursor", page.firstCursor());
        metadata.put("lastCursor", page.lastCursor());
        metadata.put("hasPreviousPage", page.hasPreviousPage());
        metadata.put("hasNextPage", page.hasNextPage());
        metadata.put("cursorResolved", page.cursorResolved());
        metadata.put("events", page.events().stream()
                .map(HermesRuntimeEvent::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }
}
