package tech.kayys.wayang.storage.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectStorageNamesTest {

    @Test
    void appliesNormalizedPrefixToLogicalKeys() {
        ObjectStorageNames names = ObjectStorageNames.fromPrefix(" /tenants/acme ");

        assertThat(names.pathPrefix()).isEqualTo("tenants/acme/");
        assertThat(names.objectName(" skills/definitions/planner.properties "))
                .isEqualTo("tenants/acme/skills/definitions/planner.properties");
        assertThat(names.objectName("/skills/artifacts/prompt/content.bin"))
                .isEqualTo("tenants/acme/skills/artifacts/prompt/content.bin");
    }

    @Test
    void stripsPrefixFromListedObjectNames() {
        ObjectStorageNames names = ObjectStorageNames.fromPrefix("tenants/acme");

        assertThat(names.logicalKey("tenants/acme/skills/events/e1.properties"))
                .isEqualTo("skills/events/e1.properties");
        assertThat(names.logicalKey(" /tenants/acme/skills/events/e2.properties "))
                .isEqualTo("skills/events/e2.properties");
        assertThat(names.logicalKey("other/events/e1.properties"))
                .isEqualTo("other/events/e1.properties");
    }

    @Test
    void supportsUnprefixedObjectNames() {
        ObjectStorageNames names = ObjectStorageNames.unprefixed();

        assertThat(names.pathPrefix()).isEmpty();
        assertThat(names.objectName(" /skills/definitions/planner.properties "))
                .isEqualTo("skills/definitions/planner.properties");
        assertThat(names.logicalKey("skills/definitions/planner.properties"))
                .isEqualTo("skills/definitions/planner.properties");
    }

    @Test
    void treatsSlashOnlyPrefixAsUnprefixed() {
        ObjectStorageNames names = ObjectStorageNames.fromPrefix("///");

        assertThat(names.pathPrefix()).isEmpty();
        assertThat(names.objectName("/skills/definitions/planner.properties"))
                .isEqualTo("skills/definitions/planner.properties");
    }
}
