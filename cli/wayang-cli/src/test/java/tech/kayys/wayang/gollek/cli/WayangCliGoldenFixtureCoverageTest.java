package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangContractCatalog;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverage;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageEntry;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageReport;
import tech.kayys.wayang.gollek.sdk.WayangContractDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangContractIndex;
import tech.kayys.wayang.gollek.sdk.WayangContractKey;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchCatalog;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommand;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandContract;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureCoverageTest {

    @Test
    void explicitSchemaManifestEntriesPointAtPublishedContracts() {
        WayangContractIndex contracts = WayangContractIndex.of(WayangContractCatalog.defaultContracts());

        for (WayangCliGoldenFixtureManifest.Entry entry : WayangCliGoldenFixtureManifest.entries()) {
            if (!entry.explicitSchema()) {
                continue;
            }
            assertThat(entry.jsonSchemaId())
                    .as(entry.name() + " jsonSchemaId")
                    .isNotBlank();
            assertThat(contracts.contractByJsonSchemaId(entry.jsonSchemaId()))
                    .as(entry.name() + " should point at a published contract")
                    .isPresent()
                    .get()
                    .satisfies(contract -> assertThat(contract)
                            .extracting(
                                    WayangContractDescriptor::schema,
                                    WayangContractDescriptor::envelope)
                            .containsExactly(entry.schema(), entry.envelope()));
        }
    }

    @Test
    void manifestCoveredContractsHaveCompleteCommandCoverage() throws IOException {
        WayangContractCommandCoverageReport coverage = WayangContractCommandCoverage.defaultCoverage();
        Set<String> fixtureJsonSchemaIds = WayangCliGoldenFixtureContracts.jsonSchemaIds();

        assertThat(fixtureJsonSchemaIds)
                .as("manifest contract schema ids")
                .isNotEmpty()
                .allSatisfy(jsonSchemaId -> assertThat(coverage.entryForJsonSchemaId(jsonSchemaId))
                        .as(jsonSchemaId + " coverage entry")
                        .isPresent()
                        .get()
                        .satisfies(WayangCliGoldenFixtureCoverageTest::assertCompleteCoverage));
    }

    @Test
    void manifestCommandLinesMapToKnownCommandFamilies() {
        Set<String> commandFamilies = WayangCliCommandFamilies.localWorkbenchCommandFamilies();
        List<String> unknown = WayangCliGoldenFixtureManifest.entries().stream()
                .filter(entry -> !commandFamilies.contains(WayangCliCommandFamilies.commandFamily(entry.args())))
                .map(WayangCliGoldenFixtureManifest.Entry::commandLine)
                .toList();

        assertThat(unknown)
                .as("golden fixture args should map to known command families")
                .isEmpty();
    }

    @Test
    void manifestCommandIdsPointAtMatchingWorkbenchCommands() throws IOException {
        Map<String, WorkbenchCommand> commandsById = localCommandsById();

        for (WayangCliGoldenFixtureManifest.Entry entry : WayangCliGoldenFixtureManifest.entries()) {
            if (!entry.schemaValidated()) {
                continue;
            }
            Set<String> fixtureContractIds = fixtureJsonSchemaIds(entry);
            for (String commandId : entry.commandIds()) {
                assertThat(commandsById)
                        .as(entry.name() + " command id " + commandId)
                        .containsKey(commandId);
                WorkbenchCommand command = commandsById.get(commandId);
                assertThat(WayangCliCommandFamilies.sameCommandFamily(command.command(), entry.args()))
                        .as(entry.name() + " should use the " + commandId + " command family")
                        .isTrue();
                List<String> commandContractIds = commandContractJsonSchemaIds(command);
                assertThat(commandContractIds)
                        .as(commandId + " declared contracts")
                        .isNotEmpty();
                assertThat(fixtureContractIds)
                        .as(entry.name() + " should emit every contract declared by " + commandId)
                        .containsAll(commandContractIds);
            }
        }
    }

    private static void assertCompleteCoverage(WayangContractCommandCoverageEntry entry) {
        assertThat(entry.commandLinked())
                .as(entry.jsonSchemaId() + " should be linked to at least one command")
                .isTrue();
        assertThat(entry.complete())
                .as(entry.jsonSchemaId() + " coverage should be complete")
                .isTrue();
        assertThat(entry.unlinkedCommandIds())
                .as(entry.jsonSchemaId() + " unlinked command ids")
                .isEmpty();
        assertThat(entry.undeclaredLinkedCommandIds())
                .as(entry.jsonSchemaId() + " undeclared linked command ids")
                .isEmpty();
    }

    private static Map<String, WorkbenchCommand> localCommandsById() {
        Map<String, WorkbenchCommand> commands = new LinkedHashMap<>();
        for (WorkbenchCommand command : WayangWorkbenchCatalog.localCommands()) {
            assertThat(commands)
                    .as("local workbench command ids should be unique")
                    .doesNotContainKey(command.id());
            commands.put(command.id(), command);
        }
        return Map.copyOf(commands);
    }

    private static List<String> commandContractJsonSchemaIds(WorkbenchCommand command) {
        return command.contracts().stream()
                .map(WorkbenchCommandContract::key)
                .map(WayangContractKey::jsonSchemaId)
                .toList();
    }

    private static Set<String> fixtureJsonSchemaIds(WayangCliGoldenFixtureManifest.Entry entry) throws IOException {
        if (entry.explicitSchema()) {
            return Set.of(entry.jsonSchemaId());
        }
        if (entry.selfDescribingSchema()) {
            return WayangCliGoldenFixtureContracts.selfDescribingJsonSchemaIds(entry.name());
        }
        return Set.of();
    }
}
