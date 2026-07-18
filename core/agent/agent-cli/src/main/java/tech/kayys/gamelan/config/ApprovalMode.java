package tech.kayys.gamelan.config;

/**
 * Controls how the agent handles tool execution approval.
 */
public enum ApprovalMode {
    AUTO("auto", "Automatically execute all tools without asking"),
    TRUSTED_TOOLS("trusted-tools", "Ask for approval on untrusted tools only"),
    ALWAYS("always", "Ask for approval before every tool execution");

    private final String value;
    private final String description;

    ApprovalMode(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String toDisplayString() { return value; }
    public String getDescription() { return description; }
}
