package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSpecAlignmentRouteRequirementsTest {

    @Test
    void emitsStandardRouteRequirementsInPinnedOrder() {
        A2aHttpRouteCatalog standard = A2aHttpRouteCatalog.standard();

        List<WayangA2aSpecAlignmentRequirement> requirements =
                WayangA2aSpecAlignmentRouteRequirements.from(standard);

        assertThat(requirements).hasSize(standard.routes().size());
        assertThat(requirements.stream().map(WayangA2aSpecAlignmentRequirement::id))
                .containsExactlyElementsOf(standard.routes().stream()
                        .map(route -> WayangA2aSpecAlignmentRouteRequirements.routeRequirementId(route.operation()))
                        .toList());
        assertThat(requirements)
                .allSatisfy(requirement -> assertThat(requirement.category()).isEqualTo("route"))
                .allSatisfy(requirement -> assertThat(requirement.aligned()).isTrue());
    }

    @Test
    void capturesCanonicalRouteShape() {
        A2aHttpRoute route = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_SEND_MESSAGE)
                .orElseThrow();

        assertThat(WayangA2aSpecAlignmentRouteRequirements.routeShape(route))
                .containsEntry("present", true)
                .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry("jsonRpcMethod", A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry("grpcMethod", A2aProtocol.OPERATION_SEND_MESSAGE)
                .containsEntry("httpMethod", "POST")
                .containsEntry("path", "/message:send")
                .containsEntry("streaming", false);
    }

    @Test
    void reportsMissingRoutesAsGaps() {
        A2aHttpRoute discover = A2aHttpRouteCatalog.standard()
                .routeForOperation(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD)
                .orElseThrow();

        List<WayangA2aSpecAlignmentRequirement> requirements =
                WayangA2aSpecAlignmentRouteRequirements.from(new A2aHttpRouteCatalog(List.of(discover)));

        WayangA2aSpecAlignmentRequirement sendMessage = requirements.stream()
                .filter(requirement -> requirement.id().equals(
                        WayangA2aSpecAlignmentRouteRequirements.routeRequirementId(A2aProtocol.OPERATION_SEND_MESSAGE)))
                .findFirst()
                .orElseThrow();
        assertThat(sendMessage.aligned()).isFalse();
        assertThat(sendMessage.category()).isEqualTo("route");
        assertThat(sendMessage.message()).contains("missing");
        assertThat(sendMessage.expected())
                .containsEntry("present", true)
                .containsEntry("operation", A2aProtocol.OPERATION_SEND_MESSAGE);
        assertThat(sendMessage.actual()).containsEntry("present", false);
    }
}
