package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesPortDispatchResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesPortResponseMapperTest {

    private final HermesPortResponseMapper mapper = new HermesPortResponseMapper();

    @Test
    void mapsSuccessfulDispatchToOkResponse() {
        Response response = mapper.dispatch(() -> new HermesPortDispatchResult(
                "runtime-diagnostics",
                "inspect",
                "summary",
                true,
                true,
                true,
                "inspected",
                "ready",
                Map.of("ready", true)));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesPortResponse body = (HermesPortResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesPortResponse::port,
                        HermesPortResponse::successful,
                        HermesPortResponse::status)
                .containsExactly("runtime-diagnostics", true, "inspected");
        assertThat(body.metadata()).containsEntry("ready", true);
    }

    @Test
    void mapsFailedDispatchToUnavailableResponse() {
        Response response = mapper.dispatch(() -> HermesPortDispatchResult.failed(
                "runtime-journal",
                "inspect",
                "latest",
                "journal unavailable",
                Map.of("store", "database")));

        assertThat(response.getStatus()).isEqualTo(503);
        HermesPortResponse body = (HermesPortResponse) response.getEntity();
        assertThat(body.successful()).isFalse();
        assertThat(body.status()).isEqualTo("failed");
        assertThat(body.metadata()).containsEntry("store", "database");
    }

    @Test
    void mapsMissingPortToTypedNotFoundError() {
        Response response = mapper.missingPort("port missing");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity()).isEqualTo(new ApiErrorResponse("port missing"));
    }

    @Test
    void mapsInvalidDispatchRequestToTypedBadRequestError() {
        Response response = mapper.dispatch(() -> {
            throw new IllegalArgumentException("bad query");
        });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo(new ApiErrorResponse("bad query"));
    }
}
