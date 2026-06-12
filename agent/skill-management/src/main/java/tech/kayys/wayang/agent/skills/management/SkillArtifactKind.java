package tech.kayys.wayang.agent.skills.management;

/**
 * Stable artifact classes used by dynamic skill persistence backends.
 */
public enum SkillArtifactKind {
    DEFINITION("definition"),
    LIFECYCLE_STATE("lifecycle-state"),
    EVENT_HISTORY("event-history"),
    PACKAGE("package"),
    RESOURCE("resource"),
    RAG_INDEX("rag-index"),
    MCP_DESCRIPTOR("mcp-descriptor");

    private final String label;

    SkillArtifactKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static SkillArtifactKind fromLabel(String label) {
        String normalized = label == null ? "" : label.trim();
        for (SkillArtifactKind kind : values()) {
            if (kind.label.equals(normalized)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown skill artifact kind label: " + label);
    }
}
