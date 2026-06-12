package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.skills.management.SkillManagementAdminViews;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsPersistenceTextRenderersTest {

    @Test
    void rendersStatusTextWithDiagnosticsAndPreflight() {
        SkillsPersistenceStatusReport report = service().report(SkillsPersistenceStatusRequest.fromOptions(
                "",
                false,
                true,
                true));
        TestConsole console = new TestConsole();

        SkillsPersistenceStatusText.render(report, console.outStream());

        assertThat(console.out())
                .contains("config source: default")
                .contains("config diagnostics:")
                .contains("lifecycle reconcile: inspect-only create-missing=false remove-orphans=false")
                .contains("persistence strategy: ephemeral")
                .contains("warnings:")
                .contains("- Disabled skill persistence roles: event-history")
                .contains("preflight: ready=true deployable=true errors=0");
    }

    @Test
    void rendersProfileCatalogText() {
        TestConsole console = new TestConsole();

        SkillsPersistenceProfileCatalogText.render(
                SkillManagementAdminViews.persistenceProfiles(),
                console.outStream());

        assertThat(console.out())
                .contains("persistence profiles: 6 durable=5 external=4 composite=2 mirrored=1 durable-fallback=2")
                .contains("- default: strategy=ephemeral durable=false roles=4 warnings=2")
                .contains("aliases: runtime,registry,memory,ephemeral,dev,development")
                .contains("- object-storage: strategy=object-storage durable=true roles=4 warnings=0")
                .contains("- mirrored-object-file: strategy=mirrored durable=true roles=4 warnings=0");
    }

    @Test
    void rendersProfileInspectionText() {
        TestConsole console = new TestConsole();
        SkillsPersistenceProfileInspectReport report = new SkillsPersistenceProfileInspectReport(
                SkillManagementAdminViews.persistenceProfile("rustfs"),
                service().report(SkillsPersistenceStatusRequest.fromOptions(
                        "rustfs",
                        false,
                        false,
                        false)));

        SkillsPersistenceProfileInspectText.render(report, console.outStream());

        assertThat(console.out())
                .contains("profile: object-storage")
                .contains("aliases: object,s3,rustfs,cloud,cloud-storage")
                .contains("description: Durable S3/RustFS-compatible object-storage profile")
                .contains("config source: profile (object-storage)")
                .contains("persistence strategy: object-storage")
                .contains("fully durable: true");
    }

    private SkillsPersistenceStatusService service() {
        return new SkillsPersistenceStatusService(
                SkillManagementServiceConfig.defaults(),
                new SkillManagementServiceFactory(new InMemoryCliSkillRegistry()));
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(out);

        PrintStream outStream() {
            return outStream;
        }

        String out() {
            return out.toString();
        }
    }
}
