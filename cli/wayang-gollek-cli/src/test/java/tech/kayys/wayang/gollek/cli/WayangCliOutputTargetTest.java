package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.gollek.sdk.WayangRunSpecService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangCliOutputTargetTest {

    @Test
    void outputTargetPrintsWhenPathIsAbsent() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        WayangCliOutputTarget.of(null, false)
                .writeOrPrint(
                        stream(output),
                        WayangRunSpecService.create()::writeProperties,
                        "Wayang run spec",
                        "specVersion=1\n");

        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("specVersion=1\n");
    }

    @Test
    void outputTargetWritesWhenPathIsPresent(@TempDir Path workspace) throws Exception {
        Path target = workspace.resolve("nested").resolve("wayang-run.properties");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        WayangCliOutputTarget.of(target.toString(), false)
                .writeOrPrint(
                        stream(output),
                        WayangRunSpecService.create()::writeProperties,
                        "Wayang run spec",
                        "prompt=test\n");

        assertThat(output.toString(StandardCharsets.UTF_8))
                .isEqualTo("Wrote Wayang run spec: " + target + System.lineSeparator());
        assertThat(Files.readString(target)).isEqualTo("prompt=test\n");
    }

    @Test
    void outputTargetRejectsBlankPath() {
        assertThatThrownBy(() -> WayangCliOutputTarget.of(" ", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("--output requires a non-empty path.");
    }

    @Test
    void outputTargetRejectsForceWithoutPath() {
        assertThatThrownBy(() -> WayangCliOutputTarget.of(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("--force requires --output.");
    }

    @Test
    void outputTargetCanGuardUnsupportedCommands() {
        WayangCliOutputTarget output = WayangCliOutputTarget.of("run.properties", false);

        assertThatThrownBy(() -> output.ensureSupported(false, "--output is only supported with --print-spec."))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("--output is only supported with --print-spec.");
    }

    @Test
    void outputTargetDoesNotGuardUnsupportedCommandsWithoutPath() {
        WayangCliOutputTarget.of(null, false)
                .ensureSupported(false, "--output is only supported with --print-spec.");
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
