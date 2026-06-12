package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

final class WayangStandardAlignmentProviderJsonSchemaProperties {

    private WayangStandardAlignmentProviderJsonSchemaProperties() {
    }

    static Map<String, Object> providerDiagnosticsProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("healthy", WayangJsonSchemaDocuments.booleanProperty())
                .required("providerCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("providerIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("providers", WayangJsonSchemaDocuments.arrayProperty(providerSummaryProperty()))
                .required("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("issues", WayangJsonSchemaDocuments.arrayProperty(providerIssueProperty()))
                .objectProperty();
    }

    static Map<String, Object> providerSummaryProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("providerId", WayangJsonSchemaDocuments.stringProperty())
                .required("providerClass", WayangJsonSchemaDocuments.stringProperty())
                .required("priority", WayangJsonSchemaDocuments.integerProperty())
                .required("standardCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("aligned", WayangJsonSchemaDocuments.booleanProperty())
                .required("gapCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .objectProperty();
    }

    static Map<String, Object> providerIssueProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("providerId", WayangJsonSchemaDocuments.stringProperty())
                .required("providerClass", WayangJsonSchemaDocuments.stringProperty())
                .required("message", WayangJsonSchemaDocuments.stringProperty())
                .objectProperty();
    }
}
