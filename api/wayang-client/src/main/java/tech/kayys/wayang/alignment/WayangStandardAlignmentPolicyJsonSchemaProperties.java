package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.client.WayangJsonSchemaObjectBuilder;

final class WayangStandardAlignmentPolicyJsonSchemaProperties {

    private WayangStandardAlignmentPolicyJsonSchemaProperties() {
    }

    static Map<String, Object> policyAssessmentProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("ready", WayangJsonSchemaDocuments.booleanProperty())
                .required("requiredStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("presentStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("missingStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("failingStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("warningStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("requiredVersions", stringMapProperty())
                .required("actualVersions", stringMapProperty())
                .required("versionMismatchStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("recommendations", WayangJsonSchemaDocuments.stringArrayProperty())
                .objectProperty();
    }

    static Map<String, Object> providerPolicyAssessmentProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("ready", WayangJsonSchemaDocuments.booleanProperty())
                .required("issueMode", WayangJsonSchemaDocuments.stringProperty())
                .required("minimumProviderCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("providerCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("requiredProviderIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("presentProviderIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("missingProviderIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("recommendations", WayangJsonSchemaDocuments.stringArrayProperty())
                .objectProperty();
    }

    private static Map<String, Object> stringMapProperty() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "object");
        values.put("additionalProperties", WayangJsonSchemaDocuments.stringProperty());
        return values;
    }
}
