package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceConfigResolveRequestTest {

    @Test
    void defaultsToDefaultSource() {
        SkillsPersistenceConfigResolveRequest request = SkillsPersistenceConfigResolveRequest.defaults();

        assertThat(request.profileName()).isEmpty();
        assertThat(request.runtimeConfig()).isFalse();
    }

    @Test
    void normalizesProfileName() {
        SkillsPersistenceConfigResolveRequest request =
                SkillsPersistenceConfigResolveRequest.fromOptions(" rustfs ", false);

        assertThat(request.profileName()).isEqualTo("rustfs");
        assertThat(request.runtimeConfig()).isFalse();
    }

    @Test
    void resolveServiceCanReceiveRequestObject() {
        SkillsPersistenceConfigResolveService service = new SkillsPersistenceConfigResolveService(
                new SkillsPersistenceConfigResolutionService(SkillManagementServiceConfig.defaults()));

        SkillsPersistenceConfigResolveReport report = service.report(
                SkillsPersistenceConfigResolveRequest.fromOptions("rustfs", false));

        assertThat(report.source()).isEqualTo("profile");
        assertThat(report.profile()).isEqualTo("object-storage");
        assertThat(report.runtime()).isFalse();
        assertThat(report.persistence().strategy()).isEqualTo("object-storage");
    }
}
