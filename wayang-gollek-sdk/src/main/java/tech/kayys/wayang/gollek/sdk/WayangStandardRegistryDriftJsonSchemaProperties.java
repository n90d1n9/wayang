package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

final class WayangStandardRegistryDriftJsonSchemaProperties {

    private WayangStandardRegistryDriftJsonSchemaProperties() {
    }

    static Map<String, Object> registryDriftProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("driftFree", WayangJsonSchemaDocuments.booleanProperty())
                .required("checkedStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("unknownStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("issues", WayangJsonSchemaDocuments.arrayProperty(registryDriftIssueProperty()))
                .objectProperty();
    }

    private static Map<String, Object> registryDriftIssueProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("standardId", WayangJsonSchemaDocuments.stringProperty())
                .required("field", WayangJsonSchemaDocuments.stringProperty())
                .required("expected", WayangJsonSchemaDocuments.stringProperty())
                .required("actual", WayangJsonSchemaDocuments.stringProperty())
                .objectProperty();
    }
}
