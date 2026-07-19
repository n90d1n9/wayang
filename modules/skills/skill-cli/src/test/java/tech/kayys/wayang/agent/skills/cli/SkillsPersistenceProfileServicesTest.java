package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillsPersistenceProfileServicesTest {

    @Test
    void catalogServiceReturnsProfileCatalog() {
        assertThat(new SkillsPersistenceProfileCatalogService().catalog().profileCount()).isEqualTo(6);
        assertThat(new SkillsPersistenceProfileCatalogService().catalog().durableProfileCount()).isEqualTo(5);
    }

    @Test
    void inspectRequestNormalizesProfileName() {
        SkillsPersistenceProfileInspectRequest request =
                SkillsPersistenceProfileInspectRequest.fromOptions(" rustfs ", true, true);

        assertThat(request.profileName()).isEqualTo("rustfs");
        assertThat(request.includePreflight()).isTrue();
        assertThat(request.includeDiagnostics()).isTrue();
    }

    @Test
    void inspectServiceComposesProfileAndStatus() {
        SkillsPersistenceProfileInspectReport report = inspectService().report(
                SkillsPersistenceProfileInspectRequest.fromOptions("rustfs", true, true));

        assertThat(report.profile().label()).isEqualTo("object-storage");
        assertThat(report.status().source()).isEqualTo("profile");
        assertThat(report.status().profile()).isEqualTo("object-storage");
        assertThat(report.status().preflightAvailable()).isTrue();
        assertThat(report.status().diagnosticsAvailable()).isTrue();
    }

    @Test
    void inspectServicePropagatesUnknownProfileErrors() {
        assertThatThrownBy(() -> inspectService().report(
                SkillsPersistenceProfileInspectRequest.fromOptions("missing-profile", false, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown skill-management service profile: missing-profile");
    }

    private SkillsPersistenceProfileInspectService inspectService() {
        return new SkillsPersistenceProfileInspectService(new SkillsPersistenceStatusService(
                SkillManagementServiceConfig.defaults(),
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry())));
    }
}
