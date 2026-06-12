package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tech.kayys.wayang.gollek.sdk.LocalWayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangPlatformCommandsTest {

    @Test
    void platformCommandsAreRegisteredFromDedicatedModule() {
        CommandLine root = commandLine();
        Map<String, CommandLine> commands = root.getSubcommands();

        assertThat((Object) commands.get("status").getCommand())
                .isInstanceOf(WayangPlatformCommands.StatusCommand.class);
        assertThat((Object) commands.get("products").getCommand())
                .isInstanceOf(WayangPlatformCommands.ProductsCommand.class);
        assertThat((Object) commands.get("sdk-boundaries").getCommand())
                .isInstanceOf(WayangPlatformCommands.SdkBoundariesCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("config").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.ConfigCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("inspect").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.InspectCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("policies").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.PoliciesCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("preflight").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.PreflightCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("providers").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.ProvidersCommand.class);
        assertThat((Object) commands.get("readiness-profiles").getSubcommands().get("sources").getCommand())
                .isInstanceOf(WayangPlatformCommands.ReadinessProfilesCommand.SourcesCommand.class);
        assertThat((Object) commands.get("profiles").getCommand())
                .isInstanceOf(WayangPlatformCommands.ProfilesCommand.class);
        assertThat((Object) commands.get("profiles").getSubcommands().get("inspect").getCommand())
                .isInstanceOf(WayangPlatformCommands.ProfilesCommand.InspectCommand.class);
    }

    @Test
    void sdkBoundariesCommandRendersBoundaryCatalogJson() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "sdk-boundaries",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"product\":\"Wayang\"")
                .contains("\"rootPackage\":\"tech.kayys.wayang.gollek.sdk\"")
                .contains("\"boundaryIds\":[\"core\",\"run\",\"context\",\"capability\"")
                .contains("\"id\":\"remote\"");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void sdkBoundariesCommandRendersSingleBoundaryText() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "sdk-boundaries",
                "run");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("Wayang SDK boundary")
                .contains("Run Lifecycle (run)")
                .contains("package: tech.kayys.wayang.gollek.sdk.run")
                .contains("contract schemas: wayang.run.planning, wayang.run.lifecycle");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void extractedProfileInspectCommandKeepsRootParentWiring() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "profiles",
                "inspect",
                "openclaw-agent",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"id\":\"openclaw-agent\"")
                .contains("\"surfaceId\":\"coding-agent\"");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void readinessProfileProvidersCommandRendersPlatformDiscoveryJson() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "readiness-profiles",
                "providers",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"product\":\"Wayang\"")
                .contains("\"ready\":true")
                .contains("\"requiredReaderTypes\":[]");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void readinessProfilePreflightCommandRendersRegistryPreflightJson() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exitCode = WayangGollekCli.execute(
                WayangGollekSdk.local(),
                stream(out),
                stream(err),
                "readiness-profiles",
                "preflight",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("\"product\":\"Wayang\"")
                .contains("\"ready\":true")
                .contains("\"providerDiscoveryRequired\":false")
                .contains("\"registryReady\":true");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void readinessProfileConfigCommandRedactsDatabaseUrlSecretsInTextOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "database",
                        "databaseUrl",
                        "jdbc:postgresql://ops:super-secret@localhost:5432/wayang?password=top-secret&token=api-secret",
                        "fallbackToBuiltIn", true))));

        int exitCode = WayangGollekCli.execute(
                sdk,
                stream(out),
                stream(err),
                "readiness-profiles",
                "config");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("password=<redacted>")
                .contains("token=<redacted>")
                .contains("ops:<redacted>@localhost")
                .doesNotContain("top-secret")
                .doesNotContain("api-secret")
                .doesNotContain("super-secret");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void readinessProfileConfigCommandRedactsObjectStorageCredentialsInJsonOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withReadinessProfileRegistry(WayangPlatformReadinessProfileRegistryConfig.fromMap(Map.of(
                        "mode", "s3",
                        "endpoint", "https://s3.example.test",
                        "bucket", "wayang",
                        "keyPrefix", "profiles/default.properties",
                        "credentials", "accessKeyId=inline-access secretAccessKey=inline-secret"))));

        int exitCode = WayangGollekCli.execute(
                sdk,
                stream(out),
                stream(err),
                "readiness-profiles",
                "config",
                "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("accessKeyId=<redacted>")
                .contains("secretAccessKey=<redacted>")
                .doesNotContain("inline-access")
                .doesNotContain("inline-secret");
        assertThat(err.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    private static CommandLine commandLine() {
        return new CommandLine(new WayangGollekCli(
                WayangGollekSdk.local(),
                stream(new ByteArrayOutputStream()),
                stream(new ByteArrayOutputStream())));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
