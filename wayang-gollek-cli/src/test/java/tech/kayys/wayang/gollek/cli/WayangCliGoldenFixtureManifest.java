package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangContractDescriptor;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class WayangCliGoldenFixtureManifest {

    private WayangCliGoldenFixtureManifest() {
    }

    static List<Entry> entries() {
        return entries(WayangCliGoldenFixtures.all());
    }

    static List<Entry> entries(List<WayangCliGoldenFixtures.GoldenFixture> fixtures) {
        return fixtures.stream()
                .map(Entry::from)
                .toList();
    }

    record Entry(
            String name,
            String sdkSource,
            int expectedExitCode,
            List<String> args,
            List<String> commandIds,
            String commandLine,
            WayangCliGoldenFixtures.SchemaMode mode,
            String schema,
            String envelope,
            String jsonSchemaId) {
        private static Entry from(WayangCliGoldenFixtures.GoldenFixture fixture) {
            WayangContractDescriptor descriptor = fixture.descriptor();
            return new Entry(
                    fixture.name(),
                    fixture.sdkSource(),
                    fixture.expectedExitCode(),
                    fixture.args(),
                    fixture.commandIds(),
                    fixture.commandLine(),
                    fixture.schemaMode(),
                    descriptor == null ? "" : descriptor.schema(),
                    descriptor == null ? "" : descriptor.envelope(),
                    descriptor == null ? "" : descriptor.jsonSchemaId());
        }

        Entry {
            args = List.copyOf(args);
            commandIds = List.copyOf(commandIds);
            mode = Objects.requireNonNull(mode, "mode");
        }

        String schemaMode() {
            return mode.name().toLowerCase(Locale.ROOT);
        }

        boolean explicitSchema() {
            return mode == WayangCliGoldenFixtures.SchemaMode.EXPLICIT;
        }

        boolean selfDescribingSchema() {
            return mode == WayangCliGoldenFixtures.SchemaMode.SELF_DESCRIBING;
        }

        boolean schemaValidated() {
            return mode.validated();
        }
    }
}
