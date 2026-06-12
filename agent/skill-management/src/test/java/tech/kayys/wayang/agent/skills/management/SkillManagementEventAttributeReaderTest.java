package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

class SkillManagementEventAttributeReaderTest {

    @Test
    void readsNormalizedFlagsCountsTextAndPrefixes() {
        SkillManagementEventAttributeReader reader = new SkillManagementEventAttributeReader(Map.of(
                DRY_RUN, "true",
                DEFINITION_CHANGES, "5",
                ARTIFACT_CHANGES, "-3",
                ARTIFACT_CONFLICTS, "not-a-number",
                PREFLIGHT_MESSAGE, "Store unavailable",
                PREFLIGHT_READY, "false",
                OPERATION_ID, "operation-1",
                PARENT_OPERATION_ID, "parent-1"));

        assertThat(reader.flag(DRY_RUN)).isTrue();
        assertThat(reader.flag(PREFLIGHT_READY)).isFalse();
        assertThat(reader.count(DEFINITION_CHANGES)).isEqualTo(5);
        assertThat(reader.count(ARTIFACT_CHANGES)).isZero();
        assertThat(reader.count(ARTIFACT_CONFLICTS)).isZero();
        assertThat(reader.text(PREFLIGHT_MESSAGE)).isEqualTo("Store unavailable");
        assertThat(reader.operationId()).isEqualTo("operation-1");
        assertThat(reader.parentOperationId()).isEqualTo("parent-1");
        assertThat(reader.hasPrefix(PREFLIGHT)).isTrue();
    }

    @Test
    void treatsMissingAttributesAsEmptyAndImmutable() {
        SkillManagementEventAttributeReader reader = new SkillManagementEventAttributeReader(null);

        assertThat(reader.flag(DRY_RUN)).isFalse();
        assertThat(reader.count(DEFINITION_CHANGES)).isZero();
        assertThat(reader.text(PREFLIGHT_MESSAGE)).isEmpty();
        assertThat(reader.operationId()).isEmpty();
        assertThat(reader.parentOperationId()).isEmpty();
        assertThat(reader.hasPrefix(PREFLIGHT)).isFalse();
        assertThatThrownBy(() -> reader.attributes().put(DRY_RUN, "true"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
