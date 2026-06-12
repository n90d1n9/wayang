package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentRequirementFactoryTest {

    @Test
    void comparesExpectedAndActualPayloads() {
        Map<String, Object> expected = Map.of("protocolVersion", "1.0");

        WayangA2aSpecAlignmentRequirement aligned = WayangA2aSpecAlignmentRequirementFactory.compare(
                "protocol.metadata",
                "protocol",
                "Protocol Metadata",
                expected,
                Map.of("protocolVersion", "1.0"),
                "metadata drifted");
        WayangA2aSpecAlignmentRequirement gap = WayangA2aSpecAlignmentRequirementFactory.compare(
                "protocol.metadata",
                "protocol",
                "Protocol Metadata",
                expected,
                Map.of("protocolVersion", "2.0"),
                "metadata drifted");

        assertThat(aligned.aligned()).isTrue();
        assertThat(aligned.message()).isBlank();
        assertThat(aligned.toMap()).doesNotContainKey("message");
        assertThat(gap.aligned()).isFalse();
        assertThat(gap.message()).isEqualTo("metadata drifted");
    }

    @Test
    void preservesExplicitAlignmentDecisions() {
        WayangA2aSpecAlignmentRequirement requirement = WayangA2aSpecAlignmentRequirementFactory.from(
                "route.sendMessage",
                "route",
                "Send Message Route",
                false,
                Map.of("path", "/message:send"),
                Map.of("path", "/message:send"),
                "route disabled by policy");

        assertThat(requirement.aligned()).isFalse();
        assertThat(requirement.expected()).containsEntry("path", "/message:send");
        assertThat(requirement.actual()).containsEntry("path", "/message:send");
        assertThat(requirement.message()).isEqualTo("route disabled by policy");
    }
}
