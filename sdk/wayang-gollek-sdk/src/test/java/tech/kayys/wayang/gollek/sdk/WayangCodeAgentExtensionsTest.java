package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCodeAgentExtensionsTest {

    @TempDir
    Path workspace;

    @Test
    void discoversSortedActiveExtensionsAndContributions() {
        WayangCodeAgentContext context = WayangCodeAgentContext.builder()
                .workspacePath(workspace)
                .projectId("demo")
                .sessionId("session-1")
                .modelId("gemma")
                .providerId("gguf")
                .metadata("tenant", "oss")
                .build();

        WayangCodeAgentExtensionDiscovery discovery = WayangCodeAgentExtensions.discover(
                context,
                List.of(new AuditExtension(), new InactiveExtension(), new BillingExtension()));

        assertThat(discovery.discoveredCount()).isEqualTo(3);
        assertThat(discovery.activeCount()).isEqualTo(2);
        assertThat(discovery.activeExtensionIds()).containsExactly("billing", "audit");
        assertThat(discovery.promptAdditions())
                .containsExactly(
                        "Enforce billing quota before expensive tool execution.",
                        "Record an audit event for every completed coding-agent run.");
        assertThat(discovery.slashCommandHints())
                .containsExactly("/billing status", "/audit tail");
        assertThat(discovery.toMap())
                .containsEntry("discoveredCount", 3)
                .containsEntry("activeCount", 2);
    }

    @Test
    void contextNormalizesDefaults() {
        WayangCodeAgentContext context = WayangCodeAgentContext.builder()
                .profileId(" ")
                .workspacePath(null)
                .modelId(" ")
                .maxSteps(0)
                .build();

        assertThat(context.profileId()).isEqualTo("coding-agent");
        assertThat(context.workspacePath()).isEqualTo(Path.of(".").toAbsolutePath().normalize());
        assertThat(context.modelId()).isEqualTo("default");
        assertThat(context.maxSteps()).isEqualTo(1);
    }

    @Test
    void sdkAndPlatformApiExposeExtensionDiscovery() {
        WayangGollekSdk local = Wayang.local();
        WayangGollekSdk sdk = (WayangGollekSdk) java.lang.reflect.Proxy.newProxyInstance(
                WayangGollekSdk.class.getClassLoader(),
                new Class<?>[] {WayangGollekSdk.class},
                (proxy, method, args) -> {
                    if ("codeAgentExtensions".equals(method.getName())) {
                        return WayangCodeAgentExtensions.discover(
                                (WayangCodeAgentContext) args[0],
                                List.of(new BillingExtension()));
                    }
                    return method.invoke(local, args);
                });
        WayangCodeAgentContext context = WayangCodeAgentContext.builder()
                .workspacePath(workspace)
                .metadata("tenant", "pro")
                .build();

        WayangCodeAgentExtensionDiscovery discovery = sdk.codeAgentExtensions(context);
        WayangPlatformApi platform = WayangClient.of(sdk).platform();
        Map<String, Object> envelope = platform.codeAgentExtensionsEnvelope(context);
        String json = platform.codeAgentExtensionsJson(context);

        assertThat(discovery.activeExtensionIds()).containsExactly("billing");
        assertThat(envelope)
                .containsEntry("product", "Wayang")
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("activeCount", 1);
        assertThat(json)
                .contains("\"activeExtensionIds\":[\"billing\"]")
                .contains("\"systemPromptAdditions\":[\"Enforce billing quota before expensive tool execution.\"]");
    }

    private static final class BillingExtension implements WayangCodeAgentExtension {
        @Override
        public String extensionId() {
            return "billing";
        }

        @Override
        public String edition() {
            return "pro";
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public List<String> capabilityTags() {
            return List.of("billing", "quota");
        }

        @Override
        public WayangCodeAgentContribution contribute(WayangCodeAgentContext context) {
            return WayangCodeAgentContribution.builder(extensionId())
                    .systemPromptAddition("Enforce billing quota before expensive tool execution.")
                    .slashCommandHint("/billing status")
                    .metadata("tenant", context.metadata().get("tenant"))
                    .build();
        }
    }

    private static final class AuditExtension implements WayangCodeAgentExtension {
        @Override
        public String extensionId() {
            return "audit";
        }

        @Override
        public String edition() {
            return "enterprise";
        }

        @Override
        public int priority() {
            return 20;
        }

        @Override
        public WayangCodeAgentContribution contribute(WayangCodeAgentContext context) {
            return WayangCodeAgentContribution.builder(extensionId())
                    .systemPromptAddition("Record an audit event for every completed coding-agent run.")
                    .slashCommandHint("/audit tail")
                    .metadata(Map.of("workspace", context.workspacePath().toString()))
                    .build();
        }
    }

    private static final class InactiveExtension implements WayangCodeAgentExtension {
        @Override
        public String extensionId() {
            return "inactive";
        }

        @Override
        public boolean supports(WayangCodeAgentContext context) {
            return false;
        }
    }
}
