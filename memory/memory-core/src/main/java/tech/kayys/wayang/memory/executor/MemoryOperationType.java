package tech.kayys.wayang.memory.executor;

import tech.kayys.gamelan.sdk.executor.core.Executor;

/**
 * Types of memory operations supported by memory executors
 */
public enum MemoryOperationType {
    /**
     * Store/write memory entry
     */
    STORE("store"),
    
    /**
     * Retrieve/read memory entries
     */
    RETRIEVE("retrieve"),
    
    /**
     * Search memory with query
     */
    SEARCH("search"),
    
    /**
     * Update existing memory entry
     */
    UPDATE("update"),
    
    /**
     * Delete memory entry
     */
    DELETE("delete"),
    
    /**
     * Clear all memory for an agent/session
     */
    CLEAR("clear"),
    
    /**
     * Get memory context/history
     */
    CONTEXT("context"),
    
    /**
     * Consolidate or summarize memories
     */
    CONSOLIDATE("consolidate"),
    
    /**
     * Get memory statistics
     */
    STATS("stats");

    private final String value;

    MemoryOperationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Check if this operation modifies memory
     */
    public boolean isWriteOperation() {
        return this == STORE || this == UPDATE || this == DELETE || this == CLEAR || this == CONSOLIDATE;
    }

    /**
     * Check if this operation reads memory
     */
    public boolean isReadOperation() {
        return this == RETRIEVE || this == SEARCH || this == CONTEXT || this == STATS;
    }

    /**
     * Parse from string value
     */
    public static MemoryOperationType fromValue(String value) {
        if (value == null) {
            return RETRIEVE;
        }
        for (MemoryOperationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return RETRIEVE;
    }
}
