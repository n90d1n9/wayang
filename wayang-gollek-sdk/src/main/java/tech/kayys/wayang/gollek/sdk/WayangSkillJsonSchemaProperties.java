package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangSkillJsonSchemaProperties {

    private WayangSkillJsonSchemaProperties() {
    }

    static Map<String, Object> discoveryProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("query", queryProperty());
        properties.put("search", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("totalSkills", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("matchingSkills", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("categories", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("categoryCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("categorySummaries", WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()));
        properties.put("sources", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("sourceCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("sourceSummaries", WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()));
        properties.put("skillIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("skills", WayangJsonSchemaDocuments.arrayProperty(skillProperty()));
        return properties;
    }

    static Map<String, Object> detailProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("product", WayangJsonSchemaDocuments.stringProperty());
        properties.put("skillId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("skill", skillProperty());
        return properties;
    }

    static Map<String, Object> discoveryProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                discoveryRequired(),
                true,
                discoveryProperties());
    }

    static Map<String, Object> detailProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                detailRequired(),
                true,
                detailProperties());
    }

    static List<String> discoveryRequired() {
        return List.of(
                "product",
                "query",
                "search",
                "totalSkills",
                "matchingSkills",
                "categories",
                "categoryCounts",
                "categorySummaries",
                "sources",
                "sourceCounts",
                "sourceSummaries",
                "skillIds",
                "skills");
    }

    static List<String> detailRequired() {
        return List.of("product", "skillId", "skill");
    }

    private static Map<String, Object> queryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("resolvedSurfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("category", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("source", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("state", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("skillId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("tag", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("inputKey", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("outputKey", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("filtered", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "surfaceId",
                        "profileId",
                        "resolvedSurfaceId",
                        "category",
                        "source",
                        "state",
                        "skillId",
                        "tag",
                        "inputKey",
                        "outputKey",
                        "filtered"),
                true,
                properties);
    }

    private static Map<String, Object> skillProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", WayangJsonSchemaDocuments.stringProperty());
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("description", WayangJsonSchemaDocuments.stringProperty());
        properties.put("category", WayangJsonSchemaDocuments.stringProperty());
        properties.put("source", WayangJsonSchemaDocuments.stringProperty());
        properties.put("version", WayangJsonSchemaDocuments.stringProperty());
        properties.put("state", WayangJsonSchemaDocuments.stringProperty());
        properties.put("availableForRuns", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("surfaceIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("inputKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("outputKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("tags", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("aliases", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "id",
                        "name",
                        "description",
                        "category",
                        "source",
                        "version",
                        "state",
                        "availableForRuns",
                        "surfaceIds",
                        "inputKeys",
                        "outputKeys",
                        "tags",
                        "aliases",
                        "metadata"),
                true,
                properties);
    }

    private static Map<String, Object> facetSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("skillIds", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("name", "count", "skillIds"),
                true,
                properties);
    }
}
