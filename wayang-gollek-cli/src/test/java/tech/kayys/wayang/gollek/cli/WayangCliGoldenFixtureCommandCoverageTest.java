package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureCommandCoverageTest {

    @Test
    void manifestCoveredCommandIdsArePublishedContractCommandIds() {
        WayangCliGoldenFixtureCommandCoverage.Report report =
                WayangCliGoldenFixtureCommandCoverage.defaultReport();

        assertThat(report.coveredCommandIds())
                .as("covered command ids")
                .isNotEmpty();
        assertThat(report.undeclaredCoveredCommandIds())
                .as("fixtures should not claim unpublished contract command ids")
                .isEmpty();
    }

    @Test
    void manifestCommandCoverageCoversPublishedCommandIds() {
        WayangCliGoldenFixtureCommandCoverage.Report report =
                WayangCliGoldenFixtureCommandCoverage.defaultReport();

        assertThat(report.missingCommandIds())
                .as("contract command ids without golden fixtures")
                .isEmpty();
    }
}
