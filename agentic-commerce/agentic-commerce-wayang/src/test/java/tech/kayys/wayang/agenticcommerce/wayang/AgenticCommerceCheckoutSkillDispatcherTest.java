package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutSkillDispatcherTest {

    @Test
    void exposesSupportedSkillIdsAndOperations() {
        AgenticCommerceCheckoutSkillDispatcher dispatcher = dispatcher();

        assertThat(dispatcher.skillIds()).containsExactlyElementsOf(AgenticCommerceWayang.checkoutSkillIds());
        assertThat(dispatcher.operations()).containsExactly(
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(dispatcher.toMap())
                .containsEntry("protocol", AgenticCommerceWayang.PROTOCOL_ID)
                .containsEntry("skillCount", 5)
                .containsEntry("operationCount", 5);
    }

    @Test
    void dispatchesBySkillIdAndOperationUsingSharedServiceState() {
        AgenticCommerceCheckoutSkillDispatcher dispatcher = dispatcher();

        Map<String, Object> created = dispatcher.executeBySkillId(
                AgenticCommerceWayang.SKILL_CREATE_CHECKOUT,
                Map.of(
                        "items",
                        List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1500)),
                        "currency",
                        "usd"))
                .await().indefinitely();
        Map<String, Object> retrieved = dispatcher.executeByOperation(
                AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                Map.of("checkout_session_id", "cs_1"))
                .await().indefinitely();

        assertThat(created)
                .containsEntry("success", true)
                .containsEntry("checkout_session_id", "cs_1")
                .containsEntry("checkout_status", "open");
        assertThat(retrieved)
                .containsEntry("success", true)
                .containsEntry("status_code", 200)
                .containsEntry("checkout_session_id", "cs_1");
    }

    @Test
    void contextDrivenExecuteRoutesByOperationOrSkillId() {
        AgenticCommerceCheckoutSkillDispatcher dispatcher = dispatcher();

        Map<String, Object> created = dispatcher.execute(Map.of(
                "operation",
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                "items",
                List.of(Map.of("id", "sku_1", "quantity", 1, "unit_amount", 1500)),
                "currency",
                "usd"))
                .await().indefinitely();
        Map<String, Object> canceled = dispatcher.execute(Map.of(
                "skillId",
                AgenticCommerceWayang.SKILL_CANCEL_CHECKOUT,
                "checkout_session_id",
                "cs_1"))
                .await().indefinitely();

        assertThat(dispatcher.canHandle(Map.of("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)))
                .isTrue();
        assertThat(dispatcher.canHandle(Map.of("skill", AgenticCommerceWayang.SKILL_CANCEL_CHECKOUT))).isTrue();
        assertThat(created).containsEntry("checkout_session_id", "cs_1");
        assertThat(canceled)
                .containsEntry("success", true)
                .containsEntry("checkout_status", "canceled");
    }

    @Test
    void unknownRoutesReturnRuntimeStyleErrors() {
        AgenticCommerceCheckoutSkillDispatcher dispatcher = dispatcher();

        Map<String, Object> missingOperation = dispatcher.executeByOperation("missing", Map.of())
                .await().indefinitely();
        Map<String, Object> missingRouting = dispatcher.execute(Map.of("currency", "usd"))
                .await().indefinitely();

        assertThat(missingOperation)
                .containsEntry("success", false)
                .containsEntry("status", "ERROR")
                .containsEntry("operation", "missing")
                .containsEntry("error", IllegalArgumentException.class.getName());
        assertThat(missingRouting)
                .containsEntry("success", false)
                .containsEntry("status", "ERROR")
                .containsEntry("error", IllegalArgumentException.class.getName());
        assertThat(String.valueOf(missingRouting.get("observation"))).contains("requires skillId");
    }

    private static AgenticCommerceCheckoutSkillDispatcher dispatcher() {
        return AgenticCommerceCheckoutSkillDispatcher.of(
                new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector()));
    }
}
