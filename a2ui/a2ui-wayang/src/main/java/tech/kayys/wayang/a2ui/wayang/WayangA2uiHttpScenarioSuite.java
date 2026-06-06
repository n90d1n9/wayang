package tech.kayys.wayang.a2ui.wayang;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Named collection of HTTP scenarios for grouped smoke and diagnostic runs.
 */
public record WayangA2uiHttpScenarioSuite(
        String id,
        List<WayangA2uiHttpScenario> scenarios,
        Map<String, Object> attributes) {

    public WayangA2uiHttpScenarioSuite {
        id = id == null || id.isBlank() ? "a2ui-http-suite" : id.trim();
        scenarios = scenarios == null
                ? List.of()
                : scenarios.stream()
                        .filter(Objects::nonNull)
                        .toList();
        attributes = WayangA2uiTransportMaps.copy(attributes);
    }

    public static WayangA2uiHttpScenarioSuite of(String id, WayangA2uiHttpScenario... scenarios) {
        return of(id, scenarios == null ? List.of() : Arrays.asList(scenarios));
    }

    public static WayangA2uiHttpScenarioSuite of(String id, List<WayangA2uiHttpScenario> scenarios) {
        return new WayangA2uiHttpScenarioSuite(id, scenarios, Map.of());
    }

    public WayangA2uiHttpScenarioSuite withAttributes(Map<?, ?> extraAttributes) {
        if (extraAttributes == null || extraAttributes.isEmpty()) {
            return this;
        }
        return new WayangA2uiHttpScenarioSuite(
                id,
                scenarios,
                WayangA2uiTransportMetadata.merge(attributes, WayangA2uiTransportMaps.copy(extraAttributes)));
    }

    public boolean empty() {
        return scenarios.isEmpty();
    }

    public int size() {
        return scenarios.size();
    }
}
