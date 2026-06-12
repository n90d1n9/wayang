package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.OPERATION_ID;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.PARENT_OPERATION_ID;

/**
 * Typed accessors for normalized skill-management event attribute maps.
 */
record SkillManagementEventAttributeReader(Map<String, String> attributes) {

    SkillManagementEventAttributeReader {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    static SkillManagementEventAttributeReader from(SkillManagementEvent event) {
        Objects.requireNonNull(event, "event");
        return new SkillManagementEventAttributeReader(event.attributes());
    }

    static SkillManagementEventAttributeReader empty() {
        return new SkillManagementEventAttributeReader(Map.of());
    }

    static SkillManagementEventAttributeReader orEmpty(SkillManagementEventAttributeReader attributes) {
        return attributes == null ? empty() : attributes;
    }

    boolean flag(String name) {
        return SkillManagementValueSupport.booleanAttribute(attributes, name);
    }

    int count(String name) {
        return SkillManagementValueSupport.nonNegativeIntAttribute(attributes, name);
    }

    boolean hasPrefix(String prefix) {
        return SkillManagementValueSupport.hasAttributePrefix(attributes, prefix);
    }

    String text(String name) {
        return SkillManagementValueSupport.text(attributes.get(name));
    }

    String operationId() {
        return text(OPERATION_ID);
    }

    String parentOperationId() {
        return text(PARENT_OPERATION_ID);
    }
}
