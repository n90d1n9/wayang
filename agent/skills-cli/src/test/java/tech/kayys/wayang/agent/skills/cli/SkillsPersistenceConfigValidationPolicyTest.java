package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceConfigValidationPolicyTest {

    private final SkillsPersistenceConfigResolutionService resolutionService =
            new SkillsPersistenceConfigResolutionService(SkillManagementServiceConfig.defaults());

    @Test
    void defaultPolicyPassesDefaultConfig() {
        SkillsPersistenceConfigValidationPolicyResult result =
                SkillsPersistenceConfigValidationPolicy.defaults().evaluate(resolveDefault());

        assertThat(result.requireDurable()).isFalse();
        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.errorCount()).isZero();
    }

    @Test
    void durablePolicyRejectsEphemeralDefaultConfig() {
        SkillsPersistenceConfigValidationPolicyResult result =
                SkillsPersistenceConfigValidationPolicy.requiringDurable(true).evaluate(resolveDefault());

        assertThat(result.requireDurable()).isTrue();
        assertThat(result.passed()).isFalse();
        assertThat(result.errors())
                .containsExactly("Fully durable skill persistence is required.");
        assertThat(result.errorCount()).isEqualTo(1);
    }

    @Test
    void durablePolicyAcceptsObjectStorageProfile() {
        SkillsPersistenceConfigValidationPolicyResult result =
                SkillsPersistenceConfigValidationPolicy.requiringDurable(true).evaluate(resolveProfile("rustfs"));

        assertThat(result.requireDurable()).isTrue();
        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.errorCount()).isZero();
    }

    @Test
    void validationServiceCanReceivePolicyObject() {
        SkillsPersistenceConfigValidationService service =
                new SkillsPersistenceConfigValidationService(resolutionService);

        SkillsPersistenceConfigValidationReport report = service.report(
                "rustfs",
                false,
                SkillsPersistenceConfigValidationPolicy.requiringDurable(true));

        assertThat(report.profile()).isEqualTo("object-storage");
        assertThat(report.requireDurable()).isTrue();
        assertThat(report.passed()).isTrue();
        assertThat(report.policyErrors()).isEmpty();
    }

    private SkillsPersistenceConfigResolution resolveDefault() {
        return resolutionService.resolve("", false);
    }

    private SkillsPersistenceConfigResolution resolveProfile(String profileName) {
        return resolutionService.resolve(profileName, false);
    }
}
