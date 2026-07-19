package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillStoreConfigKindAliasesTest {

    @Test
    void recognizesCommonBackendAliases() {
        assertThat(SkillStoreConfigKindAliases.isFilesystem("local")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isObjectStorage("s3-compatible")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isObjectStorage("RustFS")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isObjectStorage("minio")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isJdbc("database")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isCustom("external")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isHybrid("primary-fallback")).isTrue();
    }

    @Test
    void keepsStoreSpecificAliasesExplicit() {
        assertThat(SkillStoreConfigKindAliases.isDefinitionRegistry("registry")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isLifecycleMemory("volatile")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isEventNone("disabled")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isEventMemory("local")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isEventHybrid("fanout")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isEventMirrored("mirrored")).isTrue();
        assertThat(SkillStoreConfigKindAliases.isEventMirrored("dual-write")).isTrue();
    }
}
