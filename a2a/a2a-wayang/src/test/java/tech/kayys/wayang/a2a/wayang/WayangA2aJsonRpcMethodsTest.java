package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcMethodsTest {

    @Test
    void descriptorsFollowCanonicalA2aRouteCatalogOrder() {
        List<A2aHttpRoute> routes = A2aHttpRouteCatalog.standard().routes().stream()
                .filter(route -> WayangA2aMaps.optional(route.jsonRpcMethod()) != null)
                .toList();

        assertThat(WayangA2aJsonRpcMethods.descriptors()).hasSameSizeAs(routes);
        assertThat(WayangA2aJsonRpcMethods.methods())
                .containsExactlyElementsOf(routes.stream().map(A2aHttpRoute::jsonRpcMethod).toList());

        for (int index = 0; index < routes.size(); index++) {
            A2aHttpRoute route = routes.get(index);
            WayangA2aJsonRpcMethods.Descriptor descriptor =
                    WayangA2aJsonRpcMethods.descriptors().get(index);
            assertThat(descriptor)
                    .returns(route.jsonRpcMethod(), WayangA2aJsonRpcMethods.Descriptor::method)
                    .returns(route.operation(), WayangA2aJsonRpcMethods.Descriptor::operation)
                    .returns(route.grpcMethod(), WayangA2aJsonRpcMethods.Descriptor::grpcMethod)
                    .returns(route.httpMethod(), WayangA2aJsonRpcMethods.Descriptor::restMethod)
                    .returns(route.path(), WayangA2aJsonRpcMethods.Descriptor::restPath)
                    .returns(route.streaming(), WayangA2aJsonRpcMethods.Descriptor::streaming)
                    .returns(route.description(), WayangA2aJsonRpcMethods.Descriptor::description);
            assertThat(WayangA2aJsonRpcMethods.operationDescriptor(route.operation()))
                    .contains(descriptor);
        }
    }

    @Test
    void exposesTransportMetadataForBindingReports() {
        WayangA2aJsonRpcMethods.Descriptor streaming =
                WayangA2aJsonRpcMethods.requireDescriptor(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE);

        assertThat(streaming)
                .returns(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.Descriptor::method)
                .returns("/message:stream", WayangA2aJsonRpcMethods.Descriptor::restPath)
                .returns(true, WayangA2aJsonRpcMethods.Descriptor::streaming)
                .returns(A2aProtocol.EVENT_STREAM_MEDIA_TYPE,
                        WayangA2aJsonRpcMethods.Descriptor::responseMediaType);
        assertThat(WayangA2aJsonRpcMethods.streamingMethods())
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(WayangA2aJsonRpcMethods.responseMediaType(WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(WayangA2aJsonRpcMethods.responseMediaType(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))
                .isEqualTo(A2aProtocol.EVENT_STREAM_MEDIA_TYPE);

        Map<String, Object> binding = streaming.toBindingReportMap("/a2a/rpc");
        String bindingJson = WayangA2aHttpJson.write(binding);

        assertThat(binding)
                .containsEntry("method", WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE)
                .containsEntry("operation", A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE)
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("httpMethod", "POST")
                .containsEntry("requestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry("responseMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE)
                .containsEntry("streaming", true);
        assertThat(binding.keySet()).containsExactly(
                "method",
                "operation",
                "endpointPath",
                "httpMethod",
                "requestMediaType",
                "responseMediaType",
                "streaming");
        assertThat(bindingJson).startsWith("{\"method\":");
        assertThat(bindingJson.indexOf("\"operation\"")).isGreaterThan(bindingJson.indexOf("\"method\""));
        assertThat(bindingJson.indexOf("\"streaming\"")).isGreaterThan(bindingJson.indexOf("\"responseMediaType\""));
    }

    @Test
    void exposesCapabilityRequirementsFromMethodDescriptors() {
        assertThat(WayangA2aJsonRpcMethods.streaming(WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK)).isTrue();
        assertThat(WayangA2aJsonRpcMethods.streaming(WayangA2aJsonRpcMethods.GET_TASK)).isFalse();
        assertThat(WayangA2aJsonRpcMethods.streaming(A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK)).isTrue();
        assertThat(WayangA2aJsonRpcMethods.requiresPushNotificationCapability(
                        WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG))
                .isTrue();
        assertThat(WayangA2aJsonRpcMethods.requiresPushNotificationCapability(
                        A2aProtocol.OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG))
                .isTrue();
        assertThat(WayangA2aJsonRpcMethods.requiresPushNotificationCapability(
                        WayangA2aJsonRpcMethods.GET_TASK))
                .isFalse();
        assertThat(WayangA2aJsonRpcMethods.requiresExtendedAgentCardCapability(
                        WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD))
                .isTrue();
        assertThat(WayangA2aJsonRpcMethods.requiresExtendedAgentCardCapability(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .isFalse();
    }

    @Test
    void exposesFunctionalMethodGroupsForDispatchCoverage() {
        assertThat(WayangA2aJsonRpcMethods.methodGroups())
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_SEND, List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY, List.of(
                        WayangA2aJsonRpcMethods.GET_TASK,
                        WayangA2aJsonRpcMethods.LIST_TASKS))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_LIFECYCLE, List.of(
                        WayangA2aJsonRpcMethods.CANCEL_TASK))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_SUBSCRIPTION, List.of(
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_PUSH_CONFIG, List.of(
                        WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_AGENT_CARD, List.of(
                        WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD));
        assertThat(WayangA2aJsonRpcMethods.methodGroups().values().stream()
                        .flatMap(List::stream)
                        .toList())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(WayangA2aJsonRpcMethods.methodGroup(WayangA2aJsonRpcMethods.GET_TASK))
                .contains(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY);
        assertThat(WayangA2aJsonRpcMethods.methodGroup("UnknownMethod")).isEmpty();
    }

    @Test
    void rejectsUnknownMethodThroughDescriptorLookup() {
        assertThat(WayangA2aJsonRpcMethods.descriptor("UnknownMethod")).isEmpty();
        assertThat(WayangA2aJsonRpcMethods.operationDescriptor("UnknownOperation")).isEmpty();
        assertThat(WayangA2aJsonRpcMethods.operation("UnknownMethod")).isEmpty();

        assertThatThrownBy(() -> WayangA2aJsonRpcMethods.requireDescriptor("UnknownMethod"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JSON-RPC method: UnknownMethod");
    }
}
