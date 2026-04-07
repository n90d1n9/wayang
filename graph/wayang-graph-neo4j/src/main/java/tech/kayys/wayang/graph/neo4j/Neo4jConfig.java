package tech.kayys.wayang.graph.neo4j;

/**
 * Configuration for the Neo4j graph store connection.
 */
public class Neo4jConfig {

    private String uri;
    private String username;
    private String password;
    private String database;

    public Neo4jConfig() {
        this.uri = "bolt://localhost:7687";
        this.username = "neo4j";
        this.password = "password";
        this.database = "neo4j";
    }

    public Neo4jConfig(String uri, String username, String password, String database) {
        this.uri = uri;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
}
