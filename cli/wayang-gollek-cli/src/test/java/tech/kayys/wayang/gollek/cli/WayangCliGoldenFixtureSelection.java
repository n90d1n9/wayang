package tech.kayys.wayang.gollek.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class WayangCliGoldenFixtureSelection {
    static final String UPDATE_INCLUDE_PROPERTY = "wayang.golden.update.include";

    private final Set<String> includes;

    private WayangCliGoldenFixtureSelection(Set<String> includes) {
        this.includes = Collections.unmodifiableSet(new LinkedHashSet<>(includes));
    }

    static WayangCliGoldenFixtureSelection fromSystemProperties() {
        return from(System.getProperty(UPDATE_INCLUDE_PROPERTY, ""));
    }

    static WayangCliGoldenFixtureSelection from(String raw) {
        if (raw == null || raw.isBlank()) {
            return new WayangCliGoldenFixtureSelection(Set.of());
        }
        Set<String> names = new LinkedHashSet<>();
        for (String value : raw.split(",")) {
            String normalized = value.trim();
            if (!normalized.isEmpty()) {
                names.add(normalized);
            }
        }
        return new WayangCliGoldenFixtureSelection(names);
    }

    boolean selected(String name) {
        return includes.isEmpty()
                || includes.contains(name)
                || includes.contains(withoutGoldenSuffix(name));
    }

    List<String> unknownIncludes(Set<String> fixtureNames) {
        List<String> unknown = new ArrayList<>();
        for (String include : includes) {
            if (!fixtureNames.contains(include) && !fixtureNames.contains(include + ".golden")) {
                unknown.add(include);
            }
        }
        return List.copyOf(unknown);
    }

    private static String withoutGoldenSuffix(String name) {
        return name != null && name.endsWith(".golden")
                ? name.substring(0, name.length() - ".golden".length())
                : name;
    }
}
