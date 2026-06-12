package tech.kayys.wayang.agenticcommerce.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Minimal document client for database-backed Agentic Commerce persistence.
 */
public interface AgenticCommerceDatabasePersistenceClient {

    Optional<String> readText(String tableName, String documentKey);

    void writeText(String tableName, String documentKey, String mimeType, String text);

    boolean contains(String tableName, String documentKey);

    Map<String, Object> toMap();
}
