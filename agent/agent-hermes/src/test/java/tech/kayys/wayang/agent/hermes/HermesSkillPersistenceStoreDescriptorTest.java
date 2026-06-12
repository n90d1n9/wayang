package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceStoreDescriptorTest {

    @Test
    void exposesPublicAdapterContract() throws NoSuchMethodException {
        assertThat(Modifier.isPublic(HermesSkillPersistenceStoreDescriptor.class.getModifiers()))
                .isTrue();
        assertThat(HermesSkillPersistenceRoute.class.getMethod("descriptor").getReturnType())
                .isEqualTo(HermesSkillPersistenceStoreDescriptor.class);
        assertThat(Modifier.isPublic(
                HermesSkillPersistenceStoreDescriptor.class.getMethod("from", String.class).getModifiers()))
                .isTrue();
        assertThat(Modifier.isPublic(
                HermesSkillPersistenceStoreDescriptor.class.getMethod("toMetadata").getModifiers()))
                .isTrue();
    }

    @Test
    void describesDatabaseStoreCapabilities() {
        HermesSkillPersistenceStoreDescriptor descriptor =
                HermesSkillPersistenceStoreDescriptor.from("postgres");

        assertThat(descriptor.storeType()).isEqualTo("database");
        assertThat(descriptor.databaseBacked()).isTrue();
        assertThat(descriptor.cloudBacked()).isFalse();
        assertThat(descriptor.fileBacked()).isFalse();
        assertThat(descriptor.toMetadata())
                .containsEntry("storeType", "database")
                .containsEntry("databaseBacked", true)
                .containsEntry("canonicalCloudStore", "");
    }

    @Test
    void describesObjectStorageProviderCapabilities() {
        HermesSkillPersistenceStoreDescriptor descriptor =
                HermesSkillPersistenceStoreDescriptor.from("rustfs");

        assertThat(descriptor.storeType()).isEqualTo("object-storage");
        assertThat(descriptor.canonicalCloudStore()).isEqualTo("rustfs");
        assertThat(descriptor.cloudBacked()).isTrue();
        assertThat(descriptor.databaseBacked()).isFalse();
        assertThat(descriptor.toMetadata())
                .containsEntry("storeType", "object-storage")
                .containsEntry("canonicalCloudStore", "rustfs")
                .containsEntry("cloudBacked", true);
    }

    @Test
    void describesHybridAsMultiBackendCapable() {
        HermesSkillPersistenceStoreDescriptor descriptor =
                HermesSkillPersistenceStoreDescriptor.from("hybrid");

        assertThat(descriptor.hybrid()).isTrue();
        assertThat(descriptor.databaseBacked()).isTrue();
        assertThat(descriptor.cloudBacked()).isTrue();
        assertThat(descriptor.fileBacked()).isTrue();
    }
}
