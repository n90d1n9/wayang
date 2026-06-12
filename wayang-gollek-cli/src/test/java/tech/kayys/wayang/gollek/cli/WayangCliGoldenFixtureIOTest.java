package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureIOTest {

    @Test
    void skipsUnchangedFixturePayload(@TempDir Path contractsDirectory) throws IOException {
        Path fixture = contractsDirectory.resolve("unchanged.golden");
        Files.writeString(fixture, "payload" + System.lineSeparator(), StandardCharsets.UTF_8);
        FileTime originalTime = FileTime.fromMillis(123_456_789L);
        Files.setLastModifiedTime(fixture, originalTime);

        boolean written = WayangCliGoldenFixtureIO.writeFixture(contractsDirectory, "unchanged.golden", " payload ");

        assertThat(written).isFalse();
        assertThat(Files.readString(fixture, StandardCharsets.UTF_8))
                .isEqualTo("payload" + System.lineSeparator());
        assertThat(Files.getLastModifiedTime(fixture)).isEqualTo(originalTime);
    }

    @Test
    void writesNormalizedFixturePayloadWhenChanged(@TempDir Path contractsDirectory) throws IOException {
        boolean written = WayangCliGoldenFixtureIO.writeFixture(
                contractsDirectory,
                "nested/changed.golden",
                System.lineSeparator() + "changed payload" + System.lineSeparator());

        assertThat(written).isTrue();
        assertThat(Files.readString(
                        contractsDirectory.resolve("nested/changed.golden"),
                        StandardCharsets.UTF_8))
                .isEqualTo("changed payload" + System.lineSeparator());
    }
}
