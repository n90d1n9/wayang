package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangJsonSchemaObjectBuilderTest {

    @Test
    void buildsOrderedObjectPropertiesWithRequiredFields() {
        Map<String, Object> object = WayangJsonSchemaObjectBuilder.create()
                .required("alpha", WayangJsonSchemaDocuments.stringProperty())
                .optional("beta", WayangJsonSchemaDocuments.booleanProperty())
                .required("gamma", WayangJsonSchemaDocuments.integerProperty())
                .additionalProperties(false)
                .objectProperty();

        assertThat(object)
                .containsEntry("type", "object")
                .containsEntry("required", java.util.List.of("alpha", "gamma"))
                .containsEntry("additionalProperties", false);
        assertThat(objectMap(object.get("properties")).keySet())
                .containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void exposesOrderedPropertyMapForEnvelopeSchemas() {
        Map<String, Object> properties = WayangJsonSchemaObjectBuilder.create()
                .property("product", WayangJsonSchemaDocuments.stringProperty())
                .property("health", WayangJsonSchemaDocuments.openObjectProperty())
                .properties();

        assertThat(properties.keySet()).containsExactly("product", "health");
        assertThat(properties.get("product")).isEqualTo(WayangJsonSchemaDocuments.stringProperty());
    }

    @Test
    void normalizesRequiredPropertyNames() {
        Map<String, Object> object = WayangJsonSchemaObjectBuilder.create()
                .required(" name ", WayangJsonSchemaDocuments.stringProperty())
                .objectProperty();

        assertThat(object).containsEntry("required", java.util.List.of("name"));
        assertThat(objectMap(object.get("properties"))).containsOnlyKeys("name");
    }

    @Test
    void rejectsBlankAndDuplicatePropertyNames() {
        assertThatThrownBy(() -> WayangJsonSchemaObjectBuilder.create()
                        .required(" ", WayangJsonSchemaDocuments.stringProperty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schema property name is required.");

        assertThatThrownBy(() -> WayangJsonSchemaObjectBuilder.create()
                        .optional(" name ", WayangJsonSchemaDocuments.stringProperty())
                        .required("name", WayangJsonSchemaDocuments.booleanProperty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schema property 'name' is already defined.");
    }

    @Test
    void rejectsNullPropertyDefinitions() {
        assertThatThrownBy(() -> WayangJsonSchemaObjectBuilder.create().required("name", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schema property 'name' definition is required.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }
}
