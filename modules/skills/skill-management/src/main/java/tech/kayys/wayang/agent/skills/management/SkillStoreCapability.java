package tech.kayys.wayang.agent.skills.management;

/**
 * Stable capability labels reported by skill persistence stores.
 */
public enum SkillStoreCapability {
    READ("read"),
    WRITE("write"),
    DELETE("delete"),
    LIST("list"),
    QUERY_EVENTS("query-events"),
    PRUNE_EVENTS("prune-events"),
    SYNC("sync"),
    TRANSACTIONAL("transactional"),
    PRIMARY_FALLBACK("primary-fallback"),
    MIRROR_WRITE("mirror-write"),
    COMPOSITE("composite");

    private final String label;

    SkillStoreCapability(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
