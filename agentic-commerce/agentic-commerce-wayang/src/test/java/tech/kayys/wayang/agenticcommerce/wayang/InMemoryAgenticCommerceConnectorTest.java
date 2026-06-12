package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutItem;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutStatus;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCompleteCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceUpdateCheckoutSessionRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryAgenticCommerceConnectorTest {

    @Test
    void serviceRunsCheckoutLifecycleAgainstInMemoryConnector() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());

        AgenticCommerceCheckoutHttpResult created = service.create(new AgenticCommerceCreateCheckoutSessionRequest(
                null,
                List.of(new AgenticCommerceCheckoutItem("sku_1", "Wayang Hoodie", "", 2, 2500, Map.of())),
                "usd",
                null,
                Map.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "en-US",
                "UTC",
                "",
                Map.of("source", "test")));

        assertThat(created.successful()).isTrue();
        assertThat(created.checkoutSession().id()).isEqualTo("cs_1");
        assertThat(created.checkoutSession().status()).isEqualTo(AgenticCommerceCheckoutStatus.OPEN);
        assertThat(created.checkoutSession().currency()).isEqualTo("USD");
        assertThat(created.checkoutSession().totals().subtotal()).isEqualTo(5000);

        AgenticCommerceCheckoutHttpResult retrieved = service.retrieve("cs_1");
        assertThat(retrieved.successful()).isTrue();
        assertThat(retrieved.checkoutSession().id()).isEqualTo("cs_1");

        AgenticCommerceCheckoutHttpResult updated = service.update(
                "cs_1",
                new AgenticCommerceUpdateCheckoutSessionRequest(
                        null,
                        List.of(new AgenticCommerceCheckoutItem("sku_2", "Wayang Mug", "", 1, 1200, Map.of())),
                        null,
                        List.of(),
                        List.of(),
                        "",
                        List.of(),
                        Map.of(),
                        Map.of("stage", "updated")));
        assertThat(updated.successful()).isTrue();
        assertThat(updated.checkoutSession().status()).isEqualTo(AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT);
        assertThat(updated.checkoutSession().totals().total()).isEqualTo(1200);
        assertThat(updated.checkoutSession().metadata()).containsEntry("stage", "updated");

        AgenticCommerceCheckoutHttpResult completed = service.complete(
                "cs_1",
                new AgenticCommerceCompleteCheckoutSessionRequest(null, null, Map.of(), Map.of(), Map.of(), Map.of()));
        assertThat(completed.successful()).isTrue();
        assertThat(completed.checkoutSession().status()).isEqualTo(AgenticCommerceCheckoutStatus.COMPLETED);

        AgenticCommerceCheckoutHttpResult canceled = service.cancel("cs_1");
        assertThat(canceled.successful()).isTrue();
        assertThat(canceled.checkoutSession().status()).isEqualTo(AgenticCommerceCheckoutStatus.CANCELED);
    }

    @Test
    void retrieveMissingSessionReturnsProtocolError() {
        AgenticCommerceCheckoutService service = new AgenticCommerceCheckoutService(new InMemoryAgenticCommerceConnector());

        AgenticCommerceCheckoutHttpResult result = service.retrieve("cs_missing");

        assertThat(result.successful()).isFalse();
        assertThat(result.response().statusCode()).isEqualTo(404);
        assertThat(result.error().code()).isEqualTo("checkout_session_not_found");
    }
}
