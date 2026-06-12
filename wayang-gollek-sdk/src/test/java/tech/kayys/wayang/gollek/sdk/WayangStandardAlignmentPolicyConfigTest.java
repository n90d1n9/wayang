package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardAlignmentPolicyConfigTest {

    @Test
    void noneConfigBuildsEmptyPolicy() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.none();

        WayangStandardAlignmentPolicy policy = config.toPolicy();

        assertThat(config.modeId()).isEqualTo("none");
        assertThat(config.registryDriftMode()).isEqualTo(WayangStandardRegistryDriftMode.IGNORE);
        assertThat(config.requiredProviderIds()).isEmpty();
        assertThat(config.minimumProviderCount()).isZero();
        assertThat(config.providerIssueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.WARN);
        assertThat(policy.requiredStandardIds()).isEmpty();
        assertThat(policy.requiredVersions()).isEmpty();
        assertThat(config.toMap())
                .containsEntry("mode", "none")
                .containsEntry("standardIds", List.of())
                .containsEntry("registryDriftMode", "ignore")
                .containsEntry("requiredProviderIds", List.of())
                .containsEntry("minimumProviderCount", 0)
                .containsEntry("providerIssueMode", "warn");
    }

    @Test
    void strictConfigCanonicalizesRequiredStandardsAndWarningsThroughPolicy() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.builder()
                .mode("required")
                .standardIds("Agent2Agent Protocol", "agent-to-user-interface")
                .warningGapCategory("Route")
                .requiredStandardVersion("agent-to-user-interface", "v0.8")
                .build();

        WayangStandardAlignmentPolicy policy = config.toPolicy();

        assertThat(config.mode()).isEqualTo(WayangStandardAlignmentPolicyConfig.Mode.STRICT);
        assertThat(policy.requiredStandardIds()).containsExactly("a2a", "a2ui");
        assertThat(policy.warningGapCategories()).containsExactly("route");
        assertThat(policy.requiredVersions()).containsEntry("a2ui", "v0.8");
    }

    @Test
    void pinnedRegistryConfigUsesRegistryVersionsAndAllowsOverrides() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.fromMap(Map.of(
                "policyMode", "pinned-registry",
                "required_standard_ids", List.of("agent2agent", "a2ui"),
                "warning_categories", "route, diagnostic",
                "versions", Map.of("agent-to-user-interface", "v0.8-local"),
                "registry_drift_mode", "warn-only"));

        WayangStandardAlignmentPolicy policy = config.toPolicy();

        assertThat(config.mode()).isEqualTo(WayangStandardAlignmentPolicyConfig.Mode.PINNED_REGISTRY);
        assertThat(config.registryDriftMode()).isEqualTo(WayangStandardRegistryDriftMode.WARN);
        assertThat(config.warningGapCategories()).containsExactly("route", "diagnostic");
        assertThat(policy.requiredStandardIds()).containsExactly("a2a", "a2ui");
        assertThat(policy.requiredVersions())
                .containsEntry("a2a", "1.0")
                .containsEntry("a2ui", "v0.8-local");
    }

    @Test
    void pinnedKnownStandardsConfigRequiresAllRegistryStandards() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.pinnedKnownStandards();

        WayangStandardAlignmentPolicy policy = config.toPolicy();

        assertThat(config.modeId()).isEqualTo("pinnedKnownStandards");
        assertThat(policy.requiredStandardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(policy.requiredVersions())
                .containsEntry("a2a", "1.0")
                .containsEntry("a2ui", "v0.8")
                .containsEntry("agentic-commerce", "2026-01-30");
    }

    @Test
    void modeParserAcceptsPinnedKnownShorthand() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.builder()
                .mode("pinned-known")
                .build();

        assertThat(config.mode()).isEqualTo(WayangStandardAlignmentPolicyConfig.Mode.PINNED_KNOWN_STANDARDS);
        assertThat(config.toPolicy().requiredStandardIds())
                .containsExactly("a2a", "a2ui", "agentic-commerce");
    }

    @Test
    void fromMapAcceptsCommaSeparatedStandardIds() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.fromMap(Map.of(
                "mode", "strict",
                "standard_ids", "agent2agent, agenticcommerce"));

        assertThat(config.standardIds()).containsExactly("agent2agent", "agenticcommerce");
        assertThat(config.toPolicy().requiredStandardIds()).containsExactly("a2a", "agentic-commerce");
    }

    @Test
    void builderAcceptsBlockingRegistryDriftMode() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.builder()
                .mode("strict")
                .standardId("a2ui")
                .registryDriftMode("blocking")
                .build();

        assertThat(config.registryDriftMode()).isEqualTo(WayangStandardRegistryDriftMode.BLOCK);
        assertThat(config.toMap()).containsEntry("registryDriftMode", "block");
    }

    @Test
    void builderAcceptsProviderPolicyFields() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.builder()
                .requiredProviderIds("a2a-provider", "a2ui-provider")
                .minimumProviderCount(2)
                .providerIssueMode("block")
                .build();

        WayangStandardAlignmentProviderPolicy policy = config.toProviderPolicy();

        assertThat(config.requiredProviderIds()).containsExactly("a2a-provider", "a2ui-provider");
        assertThat(config.minimumProviderCount()).isEqualTo(2);
        assertThat(config.providerIssueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.BLOCK);
        assertThat(policy.requiredProviderIds()).containsExactly("a2a-provider", "a2ui-provider");
        assertThat(policy.minimumProviderCount()).isEqualTo(2);
        assertThat(policy.issueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.BLOCK);
    }

    @Test
    void fromMapParsesProviderPolicyAliases() {
        WayangStandardAlignmentPolicyConfig config = WayangStandardAlignmentPolicyConfig.fromMap(Map.of(
                "providers", "a2a-provider, a2ui-provider",
                "min_provider_count", "2",
                "provider_issue_policy", "ignore"));

        assertThat(config.requiredProviderIds()).containsExactly("a2a-provider", "a2ui-provider");
        assertThat(config.minimumProviderCount()).isEqualTo(2);
        assertThat(config.providerIssueMode()).isEqualTo(WayangStandardAlignmentProviderIssueMode.IGNORE);
        assertThat(config.toMap())
                .containsEntry("requiredProviderIds", List.of("a2a-provider", "a2ui-provider"))
                .containsEntry("minimumProviderCount", 2)
                .containsEntry("providerIssueMode", "ignore");
    }
}
