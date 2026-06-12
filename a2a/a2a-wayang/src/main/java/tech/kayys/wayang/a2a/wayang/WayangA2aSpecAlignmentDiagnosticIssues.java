package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

/**
 * Shared diagnostic issue projections for compact A2A spec-alignment snapshots.
 */
final class WayangA2aSpecAlignmentDiagnosticIssues {

    private WayangA2aSpecAlignmentDiagnosticIssues() {
    }

    static Map<String, Object> gapIssue(WayangA2aSpecAlignmentSnapshot specAlignment) {
        WayangA2aSpecAlignmentSnapshot resolved = specAlignment == null
                ? WayangA2aSpecAlignmentSnapshot.defaults()
                : specAlignment;
        return issue(
                "specAlignment",
                "spec_alignment_gaps",
                "gapCount",
                "0",
                String.valueOf(resolved.gapCount()),
                "A2A spec alignment report has " + resolved.gapCount() + " gap(s).");
    }
}
