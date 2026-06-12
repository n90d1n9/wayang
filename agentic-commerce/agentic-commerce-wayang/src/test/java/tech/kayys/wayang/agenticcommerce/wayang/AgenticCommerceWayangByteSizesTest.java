package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangByteSizesTest {

    @Test
    void parsesPlainNumbersAndFriendlyUnits() {
        assertThat(AgenticCommerceWayangByteSizes.parse("1_024", 0)).isEqualTo(1024L);
        assertThat(AgenticCommerceWayangByteSizes.parse("64kb", 0)).isEqualTo(64_000L);
        assertThat(AgenticCommerceWayangByteSizes.parse("64 KiB", 0)).isEqualTo(65_536L);
        assertThat(AgenticCommerceWayangByteSizes.parse("1.5MiB", 0)).isEqualTo(1_572_864L);
        assertThat(AgenticCommerceWayangByteSizes.parse("2 GB", 0)).isEqualTo(2_000_000_000L);
    }

    @Test
    void treatsUnlimitedAliasesAsZero() {
        assertThat(AgenticCommerceWayangByteSizes.parse("unlimited", 99)).isZero();
        assertThat(AgenticCommerceWayangByteSizes.parse("off", 99)).isZero();
        assertThat(AgenticCommerceWayangByteSizes.parse("none", 99)).isZero();
    }

    @Test
    void fallsBackForInvalidValues() {
        assertThat(AgenticCommerceWayangByteSizes.parse("", 42)).isEqualTo(42L);
        assertThat(AgenticCommerceWayangByteSizes.parse("many", 42)).isEqualTo(42L);
        assertThat(AgenticCommerceWayangByteSizes.parse("1xb", 42)).isEqualTo(42L);
    }

    @Test
    void parseReportPreservesInvalidRawValuesForValidation() {
        AgenticCommerceWayangByteSizes.ParseReport report =
                AgenticCommerceWayangByteSizes.parseReport("64xb", 0);

        assertThat(report.configured()).isTrue();
        assertThat(report.valid()).isFalse();
        assertThat(report.invalid()).isTrue();
        assertThat(report.bytes()).isZero();
        assertThat(report.rawValue()).isEqualTo("64xb");
        assertThat(report.toMap())
                .containsEntry("rawValue", "64xb")
                .containsEntry("issue", AgenticCommerceWayangByteSizes.ISSUE_INVALID_BYTE_SIZE)
                .containsEntry("bytesDisplay", "unlimited");
    }

    @Test
    void parseReportMarksBlankValuesAsUnconfigured() {
        AgenticCommerceWayangByteSizes.ParseReport report =
                AgenticCommerceWayangByteSizes.parseReport("", 42);

        assertThat(report.configured()).isFalse();
        assertThat(report.valid()).isTrue();
        assertThat(report.bytes()).isEqualTo(42L);
        assertThat(report.toMap())
                .containsEntry("configured", false)
                .containsEntry("bytesDisplay", "42 B");
    }

    @Test
    void formatsByteCountsAndLimitsForOperatorOutput() {
        assertThat(AgenticCommerceWayangByteSizes.format(0)).isEqualTo("0 B");
        assertThat(AgenticCommerceWayangByteSizes.format(512)).isEqualTo("512 B");
        assertThat(AgenticCommerceWayangByteSizes.format(1536)).isEqualTo("1.5 KiB");
        assertThat(AgenticCommerceWayangByteSizes.format(65_536)).isEqualTo("64 KiB");
        assertThat(AgenticCommerceWayangByteSizes.formatLimit(0)).isEqualTo("unlimited");
        assertThat(AgenticCommerceWayangByteSizes.formatLimit(65_536)).isEqualTo("64 KiB");
    }
}
