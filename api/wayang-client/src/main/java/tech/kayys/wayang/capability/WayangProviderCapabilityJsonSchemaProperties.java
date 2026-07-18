package tech.kayys.wayang.capability;

import java.util.List;
import java.util.Map;

final class WayangProviderCapabilityJsonSchemaProperties {

    private WayangProviderCapabilityJsonSchemaProperties() {
    }

    static Map<String, Object> discoveryProperties() {
        return discoveryObject().properties();
    }

    static Map<String, Object> detailProperties() {
        return detailObject().properties();
    }

    static Map<String, Object> discoveryProperty() {
        return discoveryObject().objectProperty();
    }

    static Map<String, Object> detailProperty() {
        return detailObject().objectProperty();
    }

    static List<String> discoveryRequired() {
        return List.of(
                "product",
                "query",
                "search",
                "totalCapabilities",
                "matchingCapabilities",
                "providerIds",
                "providerIdCounts",
                "providerSummaries",
                "moduleIds",
                "moduleIdCounts",
                "moduleSummaries",
                "capabilityTypes",
                "capabilityTypeCounts",
                "capabilityTypeSummaries",
                "standardIds",
                "standardIdCounts",
                "standardSummaries",
                "capabilityIds",
                "capabilities");
    }

    static List<String> detailRequired() {
        return List.of("product", "capabilityId", "capability");
    }

    private static WayangJsonSchemaObjectBuilder discoveryObject() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("product", WayangJsonSchemaDocuments.stringProperty())
                .required("query", queryProperty())
                .required("search", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("totalCapabilities", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("matchingCapabilities", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("providerIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("providerIdCounts", WayangJsonSchemaDocuments.countMapProperty())
                .required("providerSummaries", WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()))
                .required("moduleIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("moduleIdCounts", WayangJsonSchemaDocuments.countMapProperty())
                .required("moduleSummaries", WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()))
                .required("capabilityTypes", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("capabilityTypeCounts", WayangJsonSchemaDocuments.countMapProperty())
                .required(
                        "capabilityTypeSummaries",
                        WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()))
                .required("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("standardIdCounts", WayangJsonSchemaDocuments.countMapProperty())
                .required("standardSummaries", WayangJsonSchemaDocuments.arrayProperty(facetSummaryProperty()))
                .required("capabilityIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("capabilities", WayangJsonSchemaDocuments.arrayProperty(capabilityProperty()));
    }

    private static WayangJsonSchemaObjectBuilder detailObject() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("product", WayangJsonSchemaDocuments.stringProperty())
                .required("capabilityId", WayangJsonSchemaDocuments.stringProperty())
                .required("capability", capabilityProperty());
    }

    private static Map<String, Object> queryProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("capabilityId", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("providerId", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("providerNamespace", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("moduleId", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("capabilityType", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("state", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("standardId", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("tag", WayangJsonSchemaDocuments.nullableStringProperty())
                .required("filtered", WayangJsonSchemaDocuments.booleanProperty())
                .objectProperty();
    }

    private static Map<String, Object> capabilityProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("id", WayangJsonSchemaDocuments.stringProperty())
                .required("providerId", WayangJsonSchemaDocuments.stringProperty())
                .required("providerNamespace", WayangJsonSchemaDocuments.stringProperty())
                .required("moduleId", WayangJsonSchemaDocuments.stringProperty())
                .required("capabilityType", WayangJsonSchemaDocuments.stringProperty())
                .required("name", WayangJsonSchemaDocuments.stringProperty())
                .required("description", WayangJsonSchemaDocuments.stringProperty())
                .required("state", WayangJsonSchemaDocuments.stringProperty())
                .required("available", WayangJsonSchemaDocuments.booleanProperty())
                .required("surfaceIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("tags", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("metadata", WayangJsonSchemaDocuments.openObjectProperty())
                .objectProperty();
    }

    private static Map<String, Object> facetSummaryProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("name", WayangJsonSchemaDocuments.stringProperty())
                .required("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("capabilityIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .objectProperty();
    }
}
