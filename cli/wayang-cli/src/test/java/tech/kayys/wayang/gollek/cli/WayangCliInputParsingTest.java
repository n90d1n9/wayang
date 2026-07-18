package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangCliInputParsingTest {

    @Test
    void contextEntriesParseKeyValuePairsInOrder() {
        Map<String, Object> context = WayangCliContextEntries.parse(List.of(
                " rag.collection = docs ",
                "mcp.server=filesystem"));

        assertThat(context)
                .containsExactly(
                        entry("rag.collection", "docs"),
                        entry("mcp.server", "filesystem"));
    }

    @Test
    void contextEntriesRejectMalformedValues() {
        assertThatThrownBy(() -> WayangCliContextEntries.parse(List.of("rag.collection")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Context entries must use key=value: rag.collection");
    }

    @Test
    void requiredTextSourceUsesFallbackWhenNoSourceProvided() {
        String resolved = WayangCliTextSources.required(
                "Prompt",
                null,
                null,
                false,
                InputStream.nullInputStream(),
                "from spec");

        assertThat(resolved).isEqualTo("from spec");
    }

    @Test
    void requiredTextSourceReadsUtf8File(@TempDir Path workspace) throws Exception {
        Path prompt = workspace.resolve("prompt.txt");
        Files.writeString(prompt, "from file", StandardCharsets.UTF_8);

        String resolved = WayangCliTextSources.required(
                "Prompt",
                null,
                prompt.toString(),
                false,
                InputStream.nullInputStream());

        assertThat(resolved).isEqualTo("from file");
    }

    @Test
    void requiredTextSourceReadsStdin() {
        String resolved = WayangCliTextSources.required(
                "Prompt",
                null,
                null,
                true,
                new ByteArrayInputStream("from stdin".getBytes(StandardCharsets.UTF_8)));

        assertThat(resolved).isEqualTo("from stdin");
    }

    @Test
    void requiredTextSourceRejectsCompetingSources(@TempDir Path workspace) throws Exception {
        Path prompt = workspace.resolve("prompt.txt");
        Files.writeString(prompt, "from file", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> WayangCliTextSources.required(
                        "Prompt",
                        "inline",
                        prompt.toString(),
                        false,
                        InputStream.nullInputStream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt must come from only one source.");
    }

    @Test
    void requiredTextSourceRejectsBlankResolvedContent() {
        assertThatThrownBy(() -> WayangCliTextSources.required(
                        "Prompt",
                        null,
                        null,
                        true,
                        new ByteArrayInputStream(" ".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prompt content is empty.");
    }

    @Test
    void optionalTextSourceReturnsEmptyTextWhenNoSourceOrFallbackExists() {
        assertThat(WayangCliTextSources.optional("System prompt", null, null, null)).isEmpty();
    }
}
