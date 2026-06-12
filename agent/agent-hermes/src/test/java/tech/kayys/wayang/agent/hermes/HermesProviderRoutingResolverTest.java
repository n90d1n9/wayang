package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesProviderRoutingResolverTest {

    @Test
    void resolvesExplicitProviderAndModelHints() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Solve this with a gateway model")
                .modelId("kimi-k2.6")
                .parameter("provider", "OpenRouter")
                .parameter("highContext", true)
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("openrouter");
        assertThat(plan.requestedProvider()).isEqualTo("openrouter");
        assertThat(plan.model()).isEqualTo("kimi-k2.6");
        assertThat(plan.requestedModel()).isEqualTo("kimi-k2.6");
        assertThat(plan.explicitProvider()).isTrue();
        assertThat(plan.explicitModel()).isTrue();
        assertThat(plan.apiGatewayPreferred()).isTrue();
        assertThat(plan.highContextRequired()).isTrue();
        assertThat(plan.source()).isEqualTo("explicit");
    }

    @Test
    void selectsConfiguredPreferredProvider() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.builder()
                .preferredProvider("ollama")
                .fallbackProvider("openrouter")
                .preferLocalProviders(true)
                .build());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Run task")
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("ollama");
        assertThat(plan.fallbackProvider()).isEqualTo("openrouter");
        assertThat(plan.localPreferred()).isTrue();
        assertThat(plan.source()).isEqualTo("config");
        assertThat(plan.reason()).isEqualTo("configured provider selected");
    }

    @Test
    void infersLocalProviderFromPrompt() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Run this private task locally with Ollama")
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("ollama");
        assertThat(plan.localPreferred()).isTrue();
        assertThat(plan.apiGatewayPreferred()).isFalse();
        assertThat(plan.source()).isEqualTo("prompt");
    }

    @Test
    void infersGatewayAndHighContextNeedsFromPrompt() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Use OpenRouter for this high-context complex workstream")
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("openrouter");
        assertThat(plan.apiGatewayPreferred()).isTrue();
        assertThat(plan.highContextRequired()).isTrue();
        assertThat(plan.source()).isEqualTo("prompt");
    }

    @Test
    void keepsProviderAutomaticWhenNoHintsExist() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Prepare a release report")
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("auto");
        assertThat(plan.requestedProvider()).isEmpty();
        assertThat(plan.fallbackProvider()).isEqualTo("auto");
        assertThat(plan.localPreferred()).isFalse();
        assertThat(plan.apiGatewayPreferred()).isFalse();
        assertThat(plan.toolCallingRequired()).isTrue();
        assertThat(plan.source()).isEqualTo("none");
    }

    @Test
    void recordsExplicitModelWhileDelegatingProviderSelection() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        HermesProviderRoutingPlan plan = resolver.resolve(AgentRequest.builder()
                .parameter("model", "deepseek-v4")
                .build());

        assertThat(plan.selectedProvider()).isEqualTo("auto");
        assertThat(plan.model()).isEqualTo("deepseek-v4");
        assertThat(plan.explicitModel()).isTrue();
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).contains("explicit model requested");
    }

    @Test
    void rejectsInvalidProviderRoutingBooleans() {
        HermesProviderRoutingResolver resolver = new HermesProviderRoutingResolver(HermesAgentModeConfig.defaults());

        assertThatThrownBy(() -> resolver.resolve(AgentRequest.builder()
                .parameter("preferLocal", "maybe")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider routing boolean");
    }
}
