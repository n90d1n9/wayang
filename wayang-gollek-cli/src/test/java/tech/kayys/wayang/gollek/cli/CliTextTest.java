package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for shared Wayang CLI text normalization helpers.
 */
class CliTextTest {

    @Test
    void blankToNullTrimsTextAndCollapsesMissingValues() {
        assertThat(CliText.blankToNull(null)).isNull();
        assertThat(CliText.blankToNull("   ")).isNull();
        assertThat(CliText.blankToNull(" rag ")).isEqualTo("rag");
    }

    @Test
    void trimToDefaultUsesDefaultOnlyForBlankInput() {
        assertThat(CliText.trimToDefault(null, "fallback")).isEqualTo("fallback");
        assertThat(CliText.trimToDefault("  ", "fallback")).isEqualTo("fallback");
        assertThat(CliText.trimToDefault(" Wayang ", "fallback")).isEqualTo("Wayang");
    }

    @Test
    void commaSeparatedJoinsPresentValuesAndToleratesMissingLists() {
        assertThat(CliText.commaSeparated(List.of("rag", "mcp"))).isEqualTo("rag, mcp");
        assertThat(CliText.commaSeparated(List.of())).isEmpty();
        assertThat(CliText.commaSeparated(null)).isEmpty();
    }

    @Test
    void inlineKeyValueMapRendersEntriesInMapOrder() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surface", "coding-agent");
        values.put("steps", 12);

        assertThat(CliText.inlineKeyValueMap(values)).isEqualTo("surface=coding-agent, steps=12");
        assertThat(CliText.inlineKeyValueMap(Map.of())).isEmpty();
        assertThat(CliText.inlineKeyValueMap(null)).isEmpty();
    }

    @Test
    void yesNoRendersBooleansAndBooleanObjectsConsistently() {
        assertThat(CliText.yesNo(true)).isEqualTo("yes");
        assertThat(CliText.yesNo(false)).isEqualTo("no");
        assertThat(CliText.yesNo(Boolean.TRUE)).isEqualTo("yes");
        assertThat(CliText.yesNo(Boolean.FALSE)).isEqualTo("no");
        assertThat(CliText.yesNo(null)).isEqualTo("no");
        assertThat(CliText.yesNo("true")).isEqualTo("no");
    }

    @Test
    void appendCommaSeparatedTokenAddsDelimiterAfterFirstToken() {
        StringBuilder output = new StringBuilder();

        CliText.appendCommaSeparatedToken(output, "memory");
        CliText.appendCommaSeparatedToken(output, "workspace");
        CliText.appendCommaSeparatedToken(output, "max-steps=12");

        assertThat(output.toString()).isEqualTo("memory, workspace, max-steps=12");
    }

    @Test
    void appendCommaSeparatedTokenIfOnlyAppendsEnabledTokens() {
        StringBuilder output = new StringBuilder();

        CliText.appendCommaSeparatedTokenIf(output, "memory", true);
        CliText.appendCommaSeparatedTokenIf(output, "workspace", false);
        CliText.appendCommaSeparatedTokenIf(output, "harness", true);

        assertThat(output.toString()).isEqualTo("memory, harness");
    }

    @Test
    void appendListLineRendersOnlyNonEmptyValues() {
        StringBuilder output = new StringBuilder();

        CliText.appendListLine(output, "skills", List.of("rag", "mcp"));
        CliText.appendListLine(output, "empty", List.of());
        CliText.appendListLine(output, "missing", null);

        assertThat(output.toString()).isEqualTo("skills: rag, mcp\n");
    }

    @Test
    void appendIndentedListLineUsesCliItemIndent() {
        StringBuilder output = new StringBuilder();

        CliText.appendIndentedListLine(output, "tags", List.of("rag", "docs"));

        assertThat(output.toString()).isEqualTo("    tags: rag, docs\n");
    }

    @Test
    void appendBulletBlockRendersItemsAndExplicitEmptyPlaceholder() {
        String nl = System.lineSeparator();
        StringBuilder output = new StringBuilder();

        CliText.appendBulletBlock(output, "skills", List.of("rag", "mcp"));
        CliText.appendBulletBlock(output, "empty", List.of());
        CliText.appendBulletBlock(output, "missing", null);

        assertThat(output.toString()).isEqualTo(
                "skills:" + nl
                        + "- rag" + nl
                        + "- mcp" + nl
                        + "empty:" + nl
                        + "- none" + nl
                        + "missing:" + nl
                        + "- none" + nl);
    }

    @Test
    void appendBulletBlockIfAnySkipsEmptyValues() {
        String nl = System.lineSeparator();
        StringBuilder output = new StringBuilder();

        CliText.appendBulletBlockIfAny(output, "unknown skills", List.of("future.skill"));
        CliText.appendBulletBlockIfAny(output, "empty", List.of());
        CliText.appendBulletBlockIfAny(output, "missing", null);

        assertThat(output.toString()).isEqualTo(
                "unknown skills:" + nl
                        + "- future.skill" + nl);
    }
}
