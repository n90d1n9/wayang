package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsCommandServicesTest {

    @Test
    void composesCommandServicesFromManagementBoundary() {
        SkillsCommandServices services = services();

        assertThat(services.definitionCommandService()).isNotNull();
        assertThat(services.definitionQueryService()).isNotNull();
        assertThat(services.definitionInfoCommandService()).isNotNull();
        assertThat(services.lifecycleCommandService()).isNotNull();
        assertThat(services.profileCatalogService().catalog().profileCount()).isEqualTo(6);
        assertThat(services.configCatalogService().catalog(SkillsConfigCatalogRequest.defaults()).groups())
                .hasSize(6);
        assertThat(services.configSampleCatalogService().report().samples())
                .hasSize(12);
        assertThat(services.statusService().report(SkillsPersistenceStatusRequest.defaults()).source())
                .isEqualTo("default");
    }

    @Test
    void sharesStatusServiceWithProfileInspection() {
        SkillsPersistenceProfileInspectReport report = services().profileInspectService().report(
                SkillsPersistenceProfileInspectRequest.fromOptions("rustfs", true, true));

        assertThat(report.profile().label()).isEqualTo("object-storage");
        assertThat(report.status().profile()).isEqualTo("object-storage");
        assertThat(report.status().preflightAvailable()).isTrue();
        assertThat(report.status().diagnosticsAvailable()).isTrue();
    }

    private SkillsCommandServices services() {
        return SkillsCommandServices.from(
                new SkillManagementService(new InMemoryCliSkillRegistry()),
                null,
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry()));
    }
}
