package tech.kayys.golok.tools.spi;

/**
 * Enumerates the possible sources of tools in the golok platform.
 *
 * <p>
 * Tool sources are used to categorize tools by their origin and execution
 * mechanism:
 * <ul>
 * <li>{@link #INTERNAL_SKILL} - Built-in skills implemented within the
 * platform</li>
 * <li>{@link #MCP_SERVER} - Tools exposed via Model Context Protocol
 * servers</li>
 * <li>{@link #REST_API} - Tools backed by REST API endpoints</li>
 * <li>{@link #CLI_COMMAND} - Tools that execute command-line operations</li>
 * <li>{@link #GRPC_SERVICE} - Tools powered by gRPC services</li>
 * <li>{@link #DATABASE} - Tools that query or manipulate databases</li>
 * </ul>
 *
 * <p>
 * Each source has a unique identifier used in tool IDs and routing.
 *
 * @author golok Team
 * @version 2.0.0
 */
public enum ToolSource {
    /**
     * Internal skills implemented as part of the agent core.
     */
    INTERNAL_SKILL("skill"),

    /**
     * Tools exposed via Model Context Protocol (MCP) servers.
     */
    MCP_SERVER("mcp"),

    /**
     * Tools backed by REST API endpoints.
     */
    REST_API("rest"),

    /**
     * Tools that execute command-line operations.
     */
    CLI_COMMAND("cli"),

    /**
     * Tools powered by gRPC services.
     */
    GRPC_SERVICE("grpc"),

    /**
     * Tools that query or manipulate databases.
     */
    DATABASE("db");

    private final String id;

    ToolSource(String id) {
        this.id = id;
    }

    /**
     * Get the unique identifier for this tool source.
     *
     * @return source ID
     */
    public String id() {
        return id;
    }

    /**
     * Parse a tool source from its ID.
     *
     * @param id the source ID
     * @return the tool source
     * @throws IllegalArgumentException if ID is not recognized
     */
    public static ToolSource fromId(String id) {
        for (ToolSource source : values()) {
            if (source.id.equals(id)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown tool source ID: " + id);
    }
}
