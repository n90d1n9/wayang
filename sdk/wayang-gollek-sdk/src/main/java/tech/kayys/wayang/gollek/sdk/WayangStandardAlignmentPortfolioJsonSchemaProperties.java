package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

final class WayangStandardAlignmentPortfolioJsonSchemaProperties {

    private WayangStandardAlignmentPortfolioJsonSchemaProperties() {
    }

    static Map<String, Object> portfolioProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("aligned", WayangJsonSchemaDocuments.booleanProperty())
                .required("standardCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("alignedCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("gapCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("gapStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("standards", WayangJsonSchemaDocuments.arrayProperty(standardSummaryProperty()))
                .objectProperty();
    }

    private static Map<String, Object> standardSummaryProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("standard", standardDescriptorProperty())
                .required("aligned", WayangJsonSchemaDocuments.booleanProperty())
                .required("requirementCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("alignedCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("gapCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("gapIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("gapCategories", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("sourceCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("sources", WayangJsonSchemaDocuments.arrayProperty(sourceProperty()))
                .objectProperty();
    }

    private static Map<String, Object> standardDescriptorProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("standardId", WayangJsonSchemaDocuments.stringProperty())
                .required("name", WayangJsonSchemaDocuments.stringProperty())
                .required("version", WayangJsonSchemaDocuments.stringProperty())
                .required("binding", WayangJsonSchemaDocuments.stringProperty())
                .required("specUrl", WayangJsonSchemaDocuments.stringProperty())
                .objectProperty();
    }

    private static Map<String, Object> sourceProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("sourceType", WayangJsonSchemaDocuments.stringProperty())
                .required("sourceId", WayangJsonSchemaDocuments.stringProperty())
                .required("reportId", WayangJsonSchemaDocuments.stringProperty())
                .required("attributes", WayangJsonSchemaDocuments.openObjectProperty())
                .objectProperty();
    }
}
