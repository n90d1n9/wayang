package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceProfileInspectJsonTest {

    @Test
    void rendersProfileInspectionJsonWithEmbeddedStatus() {
        SkillsPersistenceProfileInspectReport report = new SkillsPersistenceProfileInspectReport(
                SkillManagementAdminViews.persistenceProfile("hybrid"),
                service().report(SkillsPersistenceStatusRequest.fromOptions(
                        "hybrid",
                        false,
                        false,
                        true)));

        String json = SkillsPersistenceProfileInspectJson.toJson(report);

        assertThat(json)
                .startsWith("{")
                .endsWith("}")
                .contains("\"label\":\"hybrid-object-file\"")
                .contains("\"aliases\":[\"hybrid\",\"object-file\",\"object-with-file-fallback\",\"cloud-file-fallback\"]")
                .contains("\"status\":{\"source\":\"profile\"")
                .contains("\"profile\":\"hybrid-object-file\"")
                .contains("\"diagnosticsAvailable\":true")
                .contains("\"strategy\":\"hybrid-fallback\"")
                .contains("\"roles\":[{\"role\":\"definition\"");
    }

    private SkillsPersistenceStatusService service() {
        return new SkillsPersistenceStatusService(
                SkillManagementServiceConfig.defaults(),
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry()));
    }
}
