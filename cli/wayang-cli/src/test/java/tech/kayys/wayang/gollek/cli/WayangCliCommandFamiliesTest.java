package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliCommandFamiliesTest {

    @Test
    void normalizesRunTaskCommandsWithoutTreatingPromptAsSubcommand() {
        assertThat(WayangCliCommandFamilies.commandFamily("run contract prompt --json"))
                .isEqualTo("run <task>");
        assertThat(WayangCliCommandFamilies.sameCommandFamily(
                        "run <task> --json",
                        "run contract prompt --json"))
                .isTrue();
    }

    @Test
    void preservesRunSubcommandFamilies() {
        assertThat(WayangCliCommandFamilies.commandFamily("run status contract-run-1 --json"))
                .isEqualTo("run status");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "run",
                        "cancel",
                        "contract-run-1",
                        "--reason",
                        "contract stop",
                        "--json")))
                .isEqualTo("run cancel");
        assertThat(WayangCliCommandFamilies.sameCommandFamily(
                        "run <task> --json",
                        "run status contract-run-1 --json"))
                .isFalse();
        assertThat(WayangCliCommandFamilies.sameCommandFamily(
                        "run cancel <run-id> --reason <text> --json",
                        List.of("run", "cancel", "contract-run-1", "--reason", "contract stop", "--json")))
                .isTrue();
    }

    @Test
    void normalizesOptionLedCatalogCommands() {
        assertThat(WayangCliCommandFamilies.commandFamily("contracts --domain planning --index --json"))
                .isEqualTo("contracts");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "contracts",
                        "--domain",
                        "planning",
                        "--index",
                        "--json")))
                .isEqualTo("contracts");
        assertThat(WayangCliCommandFamilies.commandFamily("skills list --surface assistant-agent --json"))
                .isEqualTo("skills list");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "skills",
                        "search",
                        "gamelan",
                        "--profile",
                        "workflow-agent",
                        "--json")))
                .isEqualTo("skills search");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of("providers", "--json")))
                .isEqualTo("providers");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of("providers", "inspect", "storage.hybrid-persistence")))
                .isEqualTo("providers inspect");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "status",
                        "--readiness-profile",
                        "default",
                        "--json")))
                .isEqualTo("status");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "commands",
                        "--contract-json-schema-id",
                        "urn:wayang:contract:wayang.run.lifecycle:v1:run-result",
                        "--json")))
                .isEqualTo("commands");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "workbench",
                        "--profile",
                        "assistant-agent",
                        "--json")))
                .isEqualTo("workbench");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "profiles",
                        "--surface",
                        "assistant-agent",
                        "--json")))
                .isEqualTo("profiles");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of(
                        "profiles",
                        "inspect",
                        "assistant-agent",
                        "--json")))
                .isEqualTo("profiles inspect");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of("products", "--json")))
                .isEqualTo("products");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of("sdk-boundaries", "--json")))
                .isEqualTo("sdk-boundaries");
        assertThat(WayangCliCommandFamilies.commandFamily(List.of("sdk-boundaries", "run", "--json")))
                .isEqualTo("sdk-boundaries");
    }
}
