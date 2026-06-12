package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.LATEST_TRAIL_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.NEXT_ACTION_QUERY_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.OUTCOME_QUERY_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.QUERY_FAILED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RECORD_FAILED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RELOAD_TRAIL_COUNT_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RELOAD_TRAILS_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETAINED_TRAIL_COUNT_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETAINED_TRAILS_MISMATCH;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.RETENTION_KEPT_OLDEST_TRAIL;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.TYPE_QUERY_MISMATCH;

/**
 * Sink/reader contract harness for transfer audit persistence adapters.
 */
public final class AgenticCommerceWayangPersistenceTransferAuditContractHarness {

    public static final String CONTRACT_ID =
            "agentic-commerce-wayang-persistence-transfer-audit-retained-history";
    public static final int DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT = 2;

    private final int expectedRetainedTrailCount;

    public AgenticCommerceWayangPersistenceTransferAuditContractHarness() {
        this(DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractHarness(int expectedRetainedTrailCount) {
        this.expectedRetainedTrailCount = Math.min(
                contractTrails().size(),
                Math.max(1, expectedRetainedTrailCount));
    }

    public static AgenticCommerceWayangPersistenceTransferAuditContractHarness retainedLatestTwo() {
        return new AgenticCommerceWayangPersistenceTransferAuditContractHarness(
                DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);
    }

    public int expectedRetainedTrailCount() {
        return expectedRetainedTrailCount;
    }

    public static List<AgenticCommerceWayangPersistenceTransferAuditTrail> contractTrailSamples() {
        return contractTrails();
    }

    public static List<String> contractJournalLineSamples() {
        return contractTrails().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditJsonl::toJsonLine)
                .toList();
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport run(
            AgenticCommerceWayangPersistenceTransferAuditSink sink) {
        return run(() -> Objects.requireNonNull(sink, "sink"), false);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport run(
            Supplier<? extends AgenticCommerceWayangPersistenceTransferAuditSink> sinkFactory) {
        return run(sinkFactory, false);
    }

    public AgenticCommerceWayangPersistenceTransferAuditContractReport run(
            Supplier<? extends AgenticCommerceWayangPersistenceTransferAuditSink> sinkFactory,
            boolean verifyReload) {
        Objects.requireNonNull(sinkFactory, "sinkFactory");
        List<String> issues = new ArrayList<>();
        Map<String, Object> attributes = new LinkedHashMap<>();
        AgenticCommerceWayangPersistenceTransferAuditSink sink = buildSink(sinkFactory, issues);
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedRetainedTrails = expectedRetainedTrails();
        AgenticCommerceWayangPersistenceTransferAuditPage retainedPage =
                AgenticCommerceWayangPersistenceTransferAuditReader.empty()
                        .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(expectedRetainedTrailCount));
        AgenticCommerceWayangPersistenceTransferAuditPage reloadPage =
                AgenticCommerceWayangPersistenceTransferAuditReader.empty()
                        .query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(expectedRetainedTrailCount));

        if (sink != null) {
            recordContractTrails(sink, issues);
            AgenticCommerceWayangPersistenceTransferAuditReader reader =
                    AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink);
            retainedPage = query(reader, AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10), issues);
            validateRetainedPage(retainedPage, expectedRetainedTrails, issues);
            validateFilterQueries(reader, expectedRetainedTrails, issues);
            if (verifyReload) {
                AgenticCommerceWayangPersistenceTransferAuditSink reloaded = buildSink(sinkFactory, issues);
                if (reloaded != null) {
                    reloadPage = query(
                            AgenticCommerceWayangPersistenceTransferAuditReader.forSink(reloaded),
                            AgenticCommerceWayangPersistenceTransferAuditQuery.latest(10),
                            issues);
                    validateReloadPage(reloadPage, expectedRetainedTrails, issues);
                }
            }
        }

        attributes.put("contractTrailTypes", contractTrails().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditTrail::trailType)
                .toList());
        attributes.put("contractTrailStatuses", contractTrails().stream()
                .map(AgenticCommerceWayangPersistenceTransferAuditTrail::trailStatus)
                .toList());
        return new AgenticCommerceWayangPersistenceTransferAuditContractReport(
                CONTRACT_ID,
                expectedRetainedTrailCount,
                retainedPage.trails().size(),
                verifyReload ? reloadPage.trails().size() : 0,
                verifyReload,
                issues,
                retainedPage.toMap(),
                verifyReload ? reloadPage.toMap() : Map.of(),
                attributes);
    }

    private AgenticCommerceWayangPersistenceTransferAuditSink buildSink(
            Supplier<? extends AgenticCommerceWayangPersistenceTransferAuditSink> sinkFactory,
            List<String> issues) {
        try {
            return Objects.requireNonNull(sinkFactory.get(), "sinkFactory result");
        } catch (RuntimeException exception) {
            issues.add(SINK_BUILD_FAILED);
            return null;
        }
    }

    private void recordContractTrails(
            AgenticCommerceWayangPersistenceTransferAuditSink sink,
            List<String> issues) {
        try {
            sink.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);
            for (AgenticCommerceWayangPersistenceTransferAuditTrail trail : contractTrails()) {
                sink.record(trail);
            }
            sink.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);
        } catch (RuntimeException exception) {
            issues.add(RECORD_FAILED);
        }
    }

    private AgenticCommerceWayangPersistenceTransferAuditPage query(
            AgenticCommerceWayangPersistenceTransferAuditReader reader,
            AgenticCommerceWayangPersistenceTransferAuditQuery query,
            List<String> issues) {
        try {
            return reader.query(query);
        } catch (RuntimeException exception) {
            issues.add(QUERY_FAILED);
            return AgenticCommerceWayangPersistenceTransferAuditReader.empty().query(query);
        }
    }

    private void validateRetainedPage(
            AgenticCommerceWayangPersistenceTransferAuditPage page,
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedRetainedTrails,
            List<String> issues) {
        if (!page.trails().equals(expectedRetainedTrails)) {
            issues.add(RETAINED_TRAILS_MISMATCH);
        }
        if (page.trails().size() != expectedRetainedTrailCount) {
            issues.add(RETAINED_TRAIL_COUNT_MISMATCH);
        }
        if (expectedRetainedTrailCount < contractTrails().size()
                && page.trails().stream()
                .anyMatch(trail -> AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT.equals(
                        trail.trailType()))) {
            issues.add(RETENTION_KEPT_OLDEST_TRAIL);
        }
        if (page.trails().isEmpty()
                || !AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY.equals(
                        page.trails().get(page.trails().size() - 1).trailType())) {
            issues.add(LATEST_TRAIL_MISMATCH);
        }
    }

    private void validateReloadPage(
            AgenticCommerceWayangPersistenceTransferAuditPage page,
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedRetainedTrails,
            List<String> issues) {
        if (!page.trails().equals(expectedRetainedTrails)) {
            issues.add(RELOAD_TRAILS_MISMATCH);
        }
        if (page.trails().size() != expectedRetainedTrailCount) {
            issues.add(RELOAD_TRAIL_COUNT_MISMATCH);
        }
    }

    private void validateFilterQueries(
            AgenticCommerceWayangPersistenceTransferAuditReader reader,
            List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedRetainedTrails,
            List<String> issues) {
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedCopyTrails = expectedRetainedTrails.stream()
                .filter(trail -> AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY.equals(
                        trail.trailType()))
                .toList();
        AgenticCommerceWayangPersistenceTransferAuditPage copyPage = query(
                reader,
                AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10),
                issues);
        if (!copyPage.trails().equals(expectedCopyTrails)) {
            issues.add(TYPE_QUERY_MISMATCH);
        }
        AgenticCommerceWayangPersistenceTransferAuditPage outcomePage = query(
                reader,
                AgenticCommerceWayangPersistenceTransferAuditQuery.byOutcome(
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE,
                        10),
                issues);
        if (!outcomePage.trails().equals(expectedRetainedTrails)) {
            issues.add(OUTCOME_QUERY_MISMATCH);
        }
        AgenticCommerceWayangPersistenceTransferAuditPage actionPage = query(
                reader,
                AgenticCommerceWayangPersistenceTransferAuditQuery.byNextAction(
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP,
                        10),
                issues);
        if (!actionPage.trails().equals(expectedRetainedTrails)) {
            issues.add(NEXT_ACTION_QUERY_MISMATCH);
        }
    }

    private List<AgenticCommerceWayangPersistenceTransferAuditTrail> expectedRetainedTrails() {
        List<AgenticCommerceWayangPersistenceTransferAuditTrail> trails = contractTrails();
        int retainedFrom = Math.max(0, trails.size() - expectedRetainedTrailCount);
        return trails.subList(retainedFrom, trails.size());
    }

    private static List<AgenticCommerceWayangPersistenceTransferAuditTrail> contractTrails() {
        return List.of(
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true),
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true),
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true));
    }

    private static AgenticCommerceWayangPersistenceTransferAuditTrail trail(
            String eventType,
            String eventStatus,
            boolean successful) {
        AgenticCommerceWayangPersistenceTransferAuditEvent event =
                new AgenticCommerceWayangPersistenceTransferAuditEvent(
                        eventType,
                        eventStatus,
                        successful,
                        false,
                        false,
                        successful,
                        false,
                        0,
                        0,
                        0,
                        1,
                        1,
                        successful ? 1 : 0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        Map.of("eventStatus", eventStatus));
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                eventType,
                List.of(event),
                Map.of("eventStatus", eventStatus));
    }
}
