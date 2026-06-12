package tech.kayys.wayang.agent.hermes;

import java.util.Comparator;
import java.util.List;

/**
 * Shared query-page assembly for ordered runtime-event readers.
 */
final class HermesRuntimeEventPages {

    private static final Comparator<HermesRuntimeEvent> TIMELINE_ORDER =
            Comparator.comparing(HermesRuntimeEvent::occurredAt)
                    .thenComparing(HermesRuntimeEvent::eventId);

    private HermesRuntimeEventPages() {
    }

    static HermesRuntimeEventPage from(
            List<HermesRuntimeEvent> orderedEvents,
            HermesRuntimeEventQuery query) {
        HermesRuntimeEventQuery resolved = query == null ? HermesRuntimeEventQuery.latest() : query;
        List<HermesRuntimeEvent> events = orderedEvents == null ? List.of() : orderedEvents.stream()
                .filter(event -> event != null)
                .toList();
        List<HermesRuntimeEvent> matched = events.stream()
                .filter(resolved::matches)
                .sorted(TIMELINE_ORDER)
                .toList();
        return page(matched, resolved);
    }

    static List<HermesRuntimeEvent> latestWindow(
            List<HermesRuntimeEvent> orderedEvents,
            int limit) {
        List<HermesRuntimeEvent> events = orderedEvents == null ? List.of() : orderedEvents.stream()
                .filter(event -> event != null)
                .toList();
        int safeLimit = Math.max(limit, 0);
        int fromIndex = Math.max(events.size() - safeLimit, 0);
        return List.copyOf(events.subList(fromIndex, events.size()));
    }

    private static HermesRuntimeEventPage page(
            List<HermesRuntimeEvent> matched,
            HermesRuntimeEventQuery query) {
        String beforeEventId = query.beforeEventId();
        String afterEventId = query.afterEventId();
        if (!afterEventId.isBlank()) {
            int index = indexOf(matched, afterEventId);
            if (index < 0) {
                return page(List.of(), 0, matched.size(), false, false, false);
            }
            int fromIndex = index + 1;
            List<HermesRuntimeEvent> candidates = matched.subList(fromIndex, matched.size());
            return pageFromRange(matched, candidates, fromIndex, query.limit(), false);
        }
        if (!beforeEventId.isBlank()) {
            int index = indexOf(matched, beforeEventId);
            if (index < 0) {
                return page(List.of(), 0, matched.size(), false, false, false);
            }
            List<HermesRuntimeEvent> candidates = matched.subList(0, index);
            return pageFromRange(matched, candidates, 0, query.limit(), true);
        }
        return pageFromRange(matched, matched, 0, query.limit(), true);
    }

    private static HermesRuntimeEventPage pageFromRange(
            List<HermesRuntimeEvent> allMatched,
            List<HermesRuntimeEvent> candidates,
            int candidateOffset,
            int limit,
            boolean latestWindow) {
        int safeLimit = Math.max(limit, 0);
        int relativeFrom = latestWindow
                ? Math.max(candidates.size() - safeLimit, 0)
                : 0;
        int relativeTo = latestWindow
                ? candidates.size()
                : Math.min(safeLimit, candidates.size());
        List<HermesRuntimeEvent> returned = List.copyOf(candidates.subList(relativeFrom, relativeTo));
        int absoluteFrom = candidateOffset + relativeFrom;
        int absoluteTo = candidateOffset + relativeTo;
        boolean hasPrevious = absoluteFrom > 0;
        boolean hasNext = absoluteTo < allMatched.size();
        return page(returned, candidates.size(), allMatched.size(), hasPrevious, hasNext, true);
    }

    private static HermesRuntimeEventPage page(
            List<HermesRuntimeEvent> returned,
            int matchedEvents,
            int totalMatchedEvents,
            boolean hasPrevious,
            boolean hasNext,
            boolean cursorResolved) {
        String firstCursor = cursor(returned, 0);
        String lastCursor = cursor(returned, returned.size() - 1);
        return new HermesRuntimeEventPage(
                returned,
                matchedEvents,
                totalMatchedEvents,
                hasPrevious ? firstCursor : "",
                hasNext ? lastCursor : "",
                firstCursor,
                lastCursor,
                hasPrevious,
                hasNext,
                cursorResolved);
    }

    private static int indexOf(List<HermesRuntimeEvent> events, String eventId) {
        String resolvedEventId = HermesText.trimToEmpty(eventId);
        if (resolvedEventId.isBlank()) {
            return -1;
        }
        for (int index = 0; index < events.size(); index++) {
            if (resolvedEventId.equals(events.get(index).eventId())) {
                return index;
            }
        }
        return -1;
    }

    private static String cursor(List<HermesRuntimeEvent> events, int index) {
        if (events == null || index < 0 || index >= events.size()) {
            return "";
        }
        return events.get(index).eventId();
    }
}
