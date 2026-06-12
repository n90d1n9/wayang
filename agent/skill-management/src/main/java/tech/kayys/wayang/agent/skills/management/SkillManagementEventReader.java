package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Optional;

/**
 * Read-side contract for querying skill-management event history.
 */
public interface SkillManagementEventReader {

    SkillManagementEventPage query(SkillManagementEventQuery query);

    default SkillManagementEventPage latest() {
        return query(SkillManagementEventQuery.latest());
    }

    static SkillManagementEventReader empty() {
        return query -> new SkillManagementEventPage(List.of(), 0);
    }

    static SkillManagementEventReader forSink(SkillManagementEventSink sink) {
        return readableSink(sink).orElseGet(SkillManagementEventReader::empty);
    }

    static Optional<SkillManagementEventReader> readableSink(SkillManagementEventSink sink) {
        return sink instanceof SkillManagementEventReader reader
                ? Optional.of(reader)
                : Optional.empty();
    }
}
