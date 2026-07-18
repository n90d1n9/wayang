package tech.kayys.wayang.catalog;

import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.client.WayangJsonSchemaObjectBuilder;

final class WayangStandardCatalogJsonSchemaProperties {

    private WayangStandardCatalogJsonSchemaProperties() {
    }

    static Map<String, Object> properties() {
        return WayangJsonSchemaObjectBuilder.create()
                .property("product", WayangJsonSchemaDocuments.stringProperty())
                .property("totalStandards", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .property("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .property("names", WayangJsonSchemaDocuments.stringArrayProperty())
                .property("versions", WayangJsonSchemaDocuments.stringArrayProperty())
                .property("bindings", WayangJsonSchemaDocuments.stringArrayProperty())
                .property("bindingCounts", WayangJsonSchemaDocuments.countMapProperty())
                .property("specUrls", WayangJsonSchemaDocuments.stringArrayProperty())
                .property("standards", WayangJsonSchemaDocuments.arrayProperty(standardProperty()))
                .properties();
    }

    private static Map<String, Object> standardProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("standardId", WayangJsonSchemaDocuments.stringProperty())
                .required("name", WayangJsonSchemaDocuments.stringProperty())
                .required("version", WayangJsonSchemaDocuments.stringProperty())
                .required("binding", WayangJsonSchemaDocuments.stringProperty())
                .required("specUrl", WayangJsonSchemaDocuments.stringProperty())
                .required("aliases", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("attributes", WayangJsonSchemaDocuments.openObjectProperty())
                .objectProperty();
    }
}
