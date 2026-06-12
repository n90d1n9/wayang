package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliCommandLinesTest {

    @Test
    void rendersSimpleArgsWithoutQuotes() {
        assertThat(WayangCliCommandLines.render(List.of("status", "--json")))
                .isEqualTo("status --json");
    }

    @Test
    void quotesArgsWithWhitespace() {
        assertThat(WayangCliCommandLines.render(List.of("run", "cancel", "run-1", "--reason", "contract stop")))
                .isEqualTo("run cancel run-1 --reason 'contract stop'");
    }

    @Test
    void escapesSingleQuotesInsideQuotedArgs() {
        assertThat(WayangCliCommandLines.render(List.of("run", "it isn't ready", "--json")))
                .isEqualTo("run 'it isn'\"'\"'t ready' --json");
    }
}
