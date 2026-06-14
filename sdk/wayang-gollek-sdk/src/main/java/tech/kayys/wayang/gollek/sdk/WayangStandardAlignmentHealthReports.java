package tech.kayys.wayang.gollek.sdk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Composition helpers for building health reports from several standard portfolios.
 */
public final class WayangStandardAlignmentHealthReports {

    private WayangStandardAlignmentHealthReports() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WayangStandardAlignmentPortfolio portfolio(
            WayangStandardAlignmentPortfolio... portfolios) {
        return portfolio(portfolios == null ? List.of() : Arrays.asList(portfolios));
    }

    public static WayangStandardAlignmentPortfolio portfolio(
            List<? extends WayangStandardAlignmentPortfolio> portfolios) {
        Builder builder = builder();
        builder.portfolios(portfolios);
        return builder.buildPortfolio();
    }

    public static WayangStandardAlignmentHealthReport configured(
            WayangStandardAlignmentPolicyConfig config,
            WayangStandardAlignmentPortfolio... portfolios) {
        return configured(config, portfolios == null ? List.of() : Arrays.asList(portfolios));
    }

    public static WayangStandardAlignmentHealthReport configured(
            WayangStandardAlignmentPolicyConfig config,
            List<? extends WayangStandardAlignmentPortfolio> portfolios) {
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(portfolio(portfolios), config);
    }

    public static WayangStandardAlignmentHealthReport configured(
            WayangStandardAlignmentPolicyConfig config,
            WayangStandardAlignmentProviderDiagnostics providerDiagnostics,
            WayangStandardAlignmentPortfolio... portfolios) {
        return configured(
                config,
                providerDiagnostics,
                portfolios == null ? List.of() : Arrays.asList(portfolios));
    }

    public static WayangStandardAlignmentHealthReport configured(
            WayangStandardAlignmentPolicyConfig config,
            WayangStandardAlignmentProviderDiagnostics providerDiagnostics,
            List<? extends WayangStandardAlignmentPortfolio> portfolios) {
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                portfolio(portfolios),
                config,
                providerDiagnostics);
    }

    public static WayangStandardAlignmentHealthReport pinnedKnownStandards(
            WayangStandardAlignmentPortfolio... portfolios) {
        return configured(WayangStandardAlignmentPolicyConfig.pinnedKnownStandards(), portfolios);
    }

    public static WayangStandardAlignmentHealthReport pinnedRegistry(
            List<String> standardIds,
            WayangStandardAlignmentPortfolio... portfolios) {
        return configured(
                WayangStandardAlignmentPolicyConfig.builder()
                        .mode(WayangStandardAlignmentPolicyConfig.Mode.PINNED_REGISTRY)
                        .standardIds(standardIds)
                        .build(),
                portfolios);
    }

    public static final class Builder {
        private final WayangStandardAlignmentPortfolio.Builder portfolio =
                WayangStandardAlignmentPortfolio.builder();
        private WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.none();
        private WayangStandardAlignmentProviderDiagnostics providerDiagnostics =
                WayangStandardAlignmentProviderDiagnostics.empty();

        private Builder() {
        }

        public Builder config(WayangStandardAlignmentPolicyConfig config) {
            this.config = config == null ? WayangStandardAlignmentPolicyConfig.none() : config;
            return this;
        }

        public Builder providerDiagnostics(WayangStandardAlignmentProviderDiagnostics providerDiagnostics) {
            this.providerDiagnostics = providerDiagnostics == null
                    ? WayangStandardAlignmentProviderDiagnostics.empty()
                    : providerDiagnostics;
            return this;
        }

        public Builder portfolio(WayangStandardAlignmentPortfolio portfolio) {
            this.portfolio.portfolio(portfolio);
            return this;
        }

        public Builder portfolios(List<? extends WayangStandardAlignmentPortfolio> portfolios) {
            if (portfolios != null) {
                portfolios.forEach(this::portfolio);
            }
            return this;
        }

        public Builder reportMap(Map<?, ?> report) {
            portfolio.reportMap(report);
            return this;
        }

        @SafeVarargs
        public final Builder reportMaps(Map<?, ?>... reports) {
            portfolio.reportMaps(reports);
            return this;
        }

        public Builder reportMaps(List<? extends Map<?, ?>> reports) {
            portfolio.reportMaps(reports);
            return this;
        }

        public WayangStandardAlignmentPortfolio buildPortfolio() {
            return portfolio.build();
        }

        public WayangStandardAlignmentHealthReport build() {
            return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(
                    buildPortfolio(),
                    config,
                    providerDiagnostics);
        }
    }
}
