package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime journal port backed by the operational journal service.
 */
public final class HermesRuntimeJournalServicePort implements HermesRuntimeJournalPort {

    private final HermesRuntimeJournalService service;

    public HermesRuntimeJournalServicePort(HermesRuntimeJournalService service) {
        this.service = service == null
                ? new HermesRuntimeJournalService(HermesRuntimeEventReader.empty())
                : service;
    }

    @Override
    public HermesPortDispatchResult inspect(HermesRuntimeJournalDirective directive) {
        HermesRuntimeJournalDirective resolved = directive == null
                ? HermesRuntimeJournalDirective.latest(HermesRuntimeEventQuery.DEFAULT_LIMIT)
                : directive;
        HermesRuntimeJournalView view = service.inspect(resolved.query());
        HermesSessionSnapshot snapshot = service.snapshot(view);
        Map<String, Object> metadata = new LinkedHashMap<>(resolved.toMetadata());
        metadata.put("journalView", view.toMetadata());
        metadata.put("summary", view.summary().toMetadata());
        metadata.put("sessionSnapshot", snapshot.toMetadata());
        metadata.put("status", snapshot.status());
        metadata.put("resumable", snapshot.resumable());
        metadata.put("requiresAttention", snapshot.requiresAttention());
        metadata.put("matchedEvents", view.page().matchedEvents());
        metadata.put("totalMatchedEvents", view.page().totalMatchedEvents());
        metadata.put("returnedEvents", view.page().returnedEvents());
        metadata.put("truncated", view.page().truncated());
        metadata.put("previousCursor", view.page().previousCursor());
        metadata.put("nextCursor", view.page().nextCursor());
        metadata.put("firstCursor", view.page().firstCursor());
        metadata.put("lastCursor", view.page().lastCursor());
        metadata.put("hasPreviousPage", view.page().hasPreviousPage());
        metadata.put("hasNextPage", view.page().hasNextPage());
        metadata.put("cursorResolved", view.page().cursorResolved());
        metadata.put("events", view.page().events().stream()
                .map(HermesRuntimeEvent::toMetadata)
                .toList());
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.RUNTIME_JOURNAL,
                resolved.operation(),
                resolved.target(),
                true,
                true,
                true,
                "inspected",
                "runtime journal inspected",
                metadata);
    }
}
