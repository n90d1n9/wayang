package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentContribution;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensionDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangCodeAgentExtensionDiscovery;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCodeExtensionTextFormatTest {

    @Test
    void rendersExtensionDiagnosticsAndCommandHints() {
        WayangCodeAgentExtensionDiscovery discovery = new WayangCodeAgentExtensionDiscovery(
                List.of(new WayangCodeAgentExtensionDiagnostics(
                        "audit",
                        "example.AuditExtension",
                        "Audit",
                        "enterprise",
                        10,
                        true,
                        List.of("audit", "tenant"),
                        "available",
                        Map.of())),
                List.of(WayangCodeAgentContribution.builder("audit")
                        .slashCommandHint("/audit tail")
                        .build()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        WayangCodeExtensionTextFormat.render(
                new PrintStream(bytes, true, StandardCharsets.UTF_8),
                false,
                discovery);

        assertThat(bytes.toString(StandardCharsets.UTF_8))
                .contains("Coding-agent extensions")
                .contains("discovered: 1")
                .contains("active:     1")
                .contains("audit [active, enterprise, priority=10]")
                .contains("capabilities: audit, tenant")
                .contains("/audit tail");
    }
}
