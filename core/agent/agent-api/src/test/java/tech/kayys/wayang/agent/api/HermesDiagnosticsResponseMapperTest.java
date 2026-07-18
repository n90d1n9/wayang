package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesAgentModeConfig;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnostics;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimePorts;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDiagnosticsResponseMapperTest {

    private final HermesDiagnosticsResponseMapper mapper = new HermesDiagnosticsResponseMapper();

    @Test
    void inspectsConfiguredDiagnosticsPort() {
        Response response = mapper.inspect(
                Optional.of(HermesRuntimeDiagnosticsPort.service(
                        HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()))),
                HermesRuntimeDiagnosticsDirective.capabilities());

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesDiagnosticsResponse::port,
                        HermesDiagnosticsResponse::target,
                        HermesDiagnosticsResponse::successful,
                        HermesDiagnosticsResponse::view)
                .containsExactly(
                        "runtime-diagnostics",
                        "runtime-diagnostics:capabilities",
                        true,
                        "capabilities");
        assertThat(body.metadata())
                .containsEntry("view", "capabilities")
                .containsKey("diagnostics");
        assertThat(body.diagnostics())
                .containsEntry("supportsPersistentMemory", true)
                .containsKey("enabledFeatures");
    }

    @Test
    void returnsNotFoundWhenDiagnosticsPortIsMissing() {
        Response response = mapper.inspect(Optional.empty(), HermesRuntimeDiagnosticsDirective.summary());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT));
    }
}
