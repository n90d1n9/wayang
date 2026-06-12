package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Read-only inspector for configured skill-management event history.
 */
public final class SkillManagementEventStoreInspector {

    public SkillManagementEventStoreInspection inspect(SkillManagementEventReader reader) {
        return inspect("events", reader);
    }

    public SkillManagementEventStoreInspection inspect(String name, SkillManagementEventReader reader) {
        SkillStoreInspectionSupport.require(reader, "reader");
        String storeType = SkillStoreInspectionSupport.storeType(reader);
        SkillStoreCapabilities capabilities = SkillStoreInspectionSupport.eventCapabilities(reader);
        List<SkillManagementEventStoreInspection> children = children(reader);
        try {
            SkillManagementEventPage page = reader.query(new SkillManagementEventQuery(
                    null,
                    "",
                    null,
                    SkillManagementEventQuery.MAX_LIMIT));
            return SkillManagementEventStoreInspection.ready(name, storeType, page, children, capabilities);
        } catch (RuntimeException error) {
            return SkillManagementEventStoreInspection.unavailable(
                    name,
                    storeType,
                    SkillStoreInspectionSupport.errorMessage(error),
                    children,
                    capabilities);
        }
    }

    private List<SkillManagementEventStoreInspection> children(SkillManagementEventReader reader) {
        if (reader instanceof MirroredSkillManagementEventSink mirrored) {
            return SkillStoreInspectionSupport.primaryFallbackChildren(
                    mirrored.primary(),
                    mirrored.fallback(),
                    this::inspectSink);
        }
        if (reader instanceof CompositeSkillManagementEventSink composite) {
            List<SkillManagementEventSink> sinks = composite.sinks();
            return java.util.stream.IntStream.range(0, sinks.size())
                    .mapToObj(index -> inspectSink(
                            SkillStoreInspectionSupport.indexedChildName(index, sinks.size(), "sink"),
                            sinks.get(index)))
                    .toList();
        }
        return List.of();
    }

    private SkillManagementEventStoreInspection inspectSink(String name, SkillManagementEventSink sink) {
        return SkillManagementEventReader.readableSink(sink)
                .map(reader -> inspect(name, reader))
                .orElseGet(() -> SkillManagementEventStoreInspection.unavailable(
                        name,
                        SkillStoreInspectionSupport.storeType(sink),
                        "Event sink does not expose readable history",
                        List.of(),
                        SkillStoreInspectionSupport.eventCapabilities(sink)));
    }
}
