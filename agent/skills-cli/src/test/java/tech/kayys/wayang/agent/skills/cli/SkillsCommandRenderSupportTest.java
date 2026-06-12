package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsCommandRenderSupportTest {

    @Test
    void rendersTextReport() {
        TestConsole console = new TestConsole();

        int exitCode = console.renderSupport().render(
                "sample",
                false,
                value -> "{\"value\":\"" + value + "\"}",
                (value, out) -> out.println("text=" + value));

        assertThat(exitCode).isZero();
        assertThat(console.out()).isEqualTo("text=sample\n");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void rendersJsonReport() {
        TestConsole console = new TestConsole();

        int exitCode = console.renderSupport().render(
                "sample",
                true,
                value -> "{\"value\":\"" + value + "\"}",
                (value, out) -> out.println("text=" + value));

        assertThat(exitCode).isZero();
        assertThat(console.out()).isEqualTo("{\"value\":\"sample\"}\n");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void returnsFailureWhenSuccessPredicateFailsAfterRendering() {
        TestConsole console = new TestConsole();

        int exitCode = console.renderSupport().render(
                "sample",
                false,
                value -> "{\"value\":\"" + value + "\"}",
                (value, out) -> out.println("text=" + value),
                value -> false);

        assertThat(exitCode).isEqualTo(1);
        assertThat(console.out()).isEqualTo("text=sample\n");
        assertThat(console.err()).isEmpty();
    }

    @Test
    void catchesIllegalArgumentExceptionFromReportSupplier() {
        TestConsole console = new TestConsole();

        int exitCode = console.renderSupport().renderSafely(
                () -> {
                    throw new IllegalArgumentException("bad input");
                },
                false,
                value -> "{\"value\":\"" + value + "\"}",
                (value, out) -> out.println("text=" + value));

        assertThat(exitCode).isEqualTo(1);
        assertThat(console.out()).isEmpty();
        assertThat(console.err()).isEqualTo("bad input\n");
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        SkillsCommandRenderSupport renderSupport() {
            return new SkillsCommandRenderSupport(new PrintStream(out), new PrintStream(err));
        }

        String out() {
            return out.toString();
        }

        String err() {
            return err.toString();
        }
    }
}
