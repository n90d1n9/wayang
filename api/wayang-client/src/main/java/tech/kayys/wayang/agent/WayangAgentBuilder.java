package tech.kayys.wayang.agent;

import tech.kayys.wayang.provider.Provider;
import tech.kayys.wayang.tools.spi.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class WayangAgentBuilder {
    private Provider provider;
    private final List<Tool> tools = new ArrayList<>();
    private String systemPrompt = "You are a helpful assistant.";
    private double temperature = 0.0;
    private int maxTokens = 4096;
    private boolean autoApproveTools = false;
    private java.nio.file.Path workspace = java.nio.file.Path.of(System.getProperty("user.dir"));

    public WayangAgentBuilder provider(Provider provider) {
        this.provider = provider;
        return this;
    }

    public WayangAgentBuilder addTool(Tool tool) {
        this.tools.add(tool);
        return this;
    }

    public WayangAgentBuilder addAllTools(List<Tool> tools) {
        this.tools.addAll(tools);
        return this;
    }

    public WayangAgentBuilder registerOsTools() {
        // Load all SPI tools
        ServiceLoader<Tool> loader = ServiceLoader.load(Tool.class);
        int before = this.tools.size();
        loader.forEach(t -> {
            System.err.println("[WayangAgentBuilder] Loaded tool via SPI: " + t.id());
            this.tools.add(t);
        });
        int after = this.tools.size();
        System.err.println("[WayangAgentBuilder] registerOsTools: loaded " + (after - before) + " tools (total=" + after + ")");
        return this;
    }

    public WayangAgentBuilder systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public WayangAgentBuilder temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public WayangAgentBuilder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public WayangAgentBuilder autoApproveTools(boolean autoApproveTools) {
        this.autoApproveTools = autoApproveTools;
        return this;
    }

    public WayangAgentBuilder workspace(java.nio.file.Path workspace) {
        this.workspace = workspace;
        return this;
    }

    public WayangAgent build() {
        if (provider == null) {
            throw new IllegalStateException("Provider must be set");
        }
        return new WayangAgent(provider, tools, systemPrompt, temperature, maxTokens, autoApproveTools, workspace);
    }
}
