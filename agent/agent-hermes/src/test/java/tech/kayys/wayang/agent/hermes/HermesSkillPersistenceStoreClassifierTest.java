package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillPersistenceStoreClassifierTest {

    @Test
    void classifiesStoreFamiliesFromCommonAliases() {
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("skill-management.definition-store"))
                .isEqualTo("skill-management");
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("postgres"))
                .isEqualTo("database");
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("local-file"))
                .isEqualTo("file");
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("rustfs"))
                .isEqualTo("object-storage");
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("s3-compatible"))
                .isEqualTo("object-storage");
        assertThat(HermesSkillPersistenceStoreClassifier.storeType("custom-vault"))
                .isEqualTo("custom");
    }

    @Test
    void canonicalizesObjectStorageProviders() {
        assertThat(HermesSkillPersistenceStoreClassifier.canonicalCloudStore("Amazon S3"))
                .isEqualTo("s3");
        assertThat(HermesSkillPersistenceStoreClassifier.canonicalCloudStore("S3-compatible"))
                .isEqualTo("s3-compatible");
        assertThat(HermesSkillPersistenceStoreClassifier.canonicalCloudStore("RustFS"))
                .isEqualTo("rustfs");
        assertThat(HermesSkillPersistenceStoreClassifier.canonicalCloudStore("object-store"))
                .isEqualTo("object-storage");
        assertThat(HermesSkillPersistenceStoreClassifier.canonicalCloudStore("database"))
                .isNull();
    }

    @Test
    void comparesStoresByNormalizedIdentity() {
        assertThat(HermesSkillPersistenceStoreClassifier.sameStore("file-system", "file_system"))
                .isTrue();
        assertThat(HermesSkillPersistenceStoreClassifier.sameStore("s3-compatible", "s3 compatible"))
                .isTrue();
        assertThat(HermesSkillPersistenceStoreClassifier.sameStore("database", "s3"))
                .isFalse();
    }
}
