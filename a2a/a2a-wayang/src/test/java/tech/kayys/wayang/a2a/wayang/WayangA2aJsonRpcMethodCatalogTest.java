package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodCatalogTest {

    @Test
    void exposesTheSameCanonicalCatalogAsPublicFacade() {
        assertThat(WayangA2aJsonRpcMethodCatalog.descriptors())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.descriptors());
        assertThat(WayangA2aJsonRpcMethodCatalog.methods())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(WayangA2aJsonRpcMethodCatalog.methodGroups())
                .isEqualTo(WayangA2aJsonRpcMethods.methodGroups());
        assertThat(WayangA2aJsonRpcMethodCatalog.streamingMethods())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.streamingMethods());
    }

    @Test
    void indexesDescriptorsByMethodAndOperation() {
        WayangA2aJsonRpcMethods.Descriptor send =
                WayangA2aJsonRpcMethods.requireDescriptor(WayangA2aJsonRpcMethods.SEND_MESSAGE);

        assertThat(WayangA2aJsonRpcMethodCatalog.descriptor(WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .contains(send);
        assertThat(WayangA2aJsonRpcMethodCatalog.operationDescriptor(A2aProtocol.OPERATION_SEND_MESSAGE))
                .contains(send);
        assertThat(WayangA2aJsonRpcMethodCatalog.descriptorForMethodOrOperation(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .contains(send);
        assertThat(WayangA2aJsonRpcMethodCatalog.descriptorForMethodOrOperation(
                        A2aProtocol.OPERATION_SEND_MESSAGE))
                .contains(send);
        assertThat(WayangA2aJsonRpcMethodCatalog.descriptorForMethodOrOperation("UnknownMethod"))
                .isEmpty();
    }

    @Test
    void keepsFunctionalGroupAssignmentsCompleteAndDistinct() {
        assertThat(WayangA2aJsonRpcMethodCatalog.methodGroups())
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_SEND, List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE))
                .containsEntry(WayangA2aJsonRpcMethods.METHOD_GROUP_PUSH_CONFIG, List.of(
                        WayangA2aJsonRpcMethods.CREATE_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG,
                        WayangA2aJsonRpcMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIGS,
                        WayangA2aJsonRpcMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG));
        assertThat(WayangA2aJsonRpcMethodCatalog.methodGroups().values().stream()
                        .flatMap(List::stream)
                        .toList())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethodCatalog.methods());
        assertThat(WayangA2aJsonRpcMethodCatalog.methodGroup(WayangA2aJsonRpcMethods.CANCEL_TASK))
                .contains(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_LIFECYCLE);
    }
}
