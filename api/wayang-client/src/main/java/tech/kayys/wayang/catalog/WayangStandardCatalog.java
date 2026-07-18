package tech.kayys.wayang.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import tech.kayys.wayang.client.SdkFacets;
import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangStandardDefinition;
import tech.kayys.wayang.registry.WayangStandardRegistry;

/**
 * Stable catalog view of the standards Wayang knows how to align against.
 */
public record WayangStandardCatalog(List<WayangStandardDefinition> standards) {

    public WayangStandardCatalog {
        standards = SdkLists.copy(standards);
    }

    public static WayangStandardCatalog defaultCatalog() {
        return new WayangStandardCatalog(WayangStandardRegistry.knownStandards());
    }

    public int totalStandards() {
        return standards.size();
    }

    public List<String> standardIds() {
        return standards.stream()
                .map(WayangStandardDefinition::standardId)
                .toList();
    }

    public List<String> names() {
        return standards.stream()
                .map(WayangStandardDefinition::name)
                .toList();
    }

    public List<String> versions() {
        return SdkFacets.textValues(standards, WayangStandardDefinition::version);
    }

    public List<String> bindings() {
        return SdkFacets.textValues(standards, WayangStandardDefinition::binding);
    }

    public Map<String, Integer> bindingCounts() {
        return SdkFacets.textCounts(standards, WayangStandardDefinition::binding);
    }

    public List<String> specUrls() {
        return SdkFacets.textValues(standards, WayangStandardDefinition::specUrl);
    }

    public Optional<WayangStandardDefinition> standard(String idOrAlias) {
        String normalized = normalizeKey(idOrAlias);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return standards.stream()
                .filter(definition -> matches(definition, normalized))
                .findFirst();
    }

    public boolean contains(String idOrAlias) {
        return standard(idOrAlias).isPresent();
    }

    private static boolean matches(WayangStandardDefinition definition, String normalized) {
        if (normalizeKey(definition.standardId()).equals(normalized)
                || normalizeKey(definition.name()).equals(normalized)) {
            return true;
        }
        return definition.aliases().stream()
                .map(WayangStandardCatalog::normalizeKey)
                .anyMatch(normalized::equals);
    }

    private static String normalizeKey(String value) {
        String normalized = SdkText.trimToEmpty(value).toLowerCase();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replaceAll("[^a-z0-9]+", "");
    }
}
