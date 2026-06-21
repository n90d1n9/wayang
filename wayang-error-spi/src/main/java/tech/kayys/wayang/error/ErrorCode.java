/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.error;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Central registry for Wayang error codes.
 *
 * <p>
 * Pattern: CATEGORY_NNN (example: CORE_001)
 * </p>
 */
public enum ErrorCode {

    // ===== Core Errors =====
    CORE_INVALID_REQUEST(ErrorCategory.CORE, 400, "CORE_001", "Invalid request", false),
    CORE_NOT_FOUND(ErrorCategory.CORE, 404, "CORE_002", "Resource not found", false),
    CORE_UNSUPPORTED(ErrorCategory.CORE, 400, "CORE_003", "Unsupported operation", false),
    CORE_CONFLICT(ErrorCategory.CORE, 409, "CORE_004", "Conflict detected", false),

    // ===== Orchestration Errors =====
    ORCHESTRATION_WORKFLOW_NOT_FOUND(ErrorCategory.ORCHESTRATION, 404, "ORCH_001", "Workflow not found", false),
    ORCHESTRATION_STATE_INVALID(ErrorCategory.ORCHESTRATION, 409, "ORCH_002", "Invalid orchestration state", false),
    ORCHESTRATION_SCHEDULE_FAILED(ErrorCategory.ORCHESTRATION, 500, "ORCH_003", "Failed to schedule workflow", true),

    // ===== Execution Errors =====
    EXECUTION_TASK_NOT_FOUND(ErrorCategory.EXECUTION, 404, "EXEC_001", "Task execution not found", false),
    EXECUTION_DISPATCH_FAILED(ErrorCategory.EXECUTION, 502, "EXEC_002", "Task dispatch failed", true),
    EXECUTION_TIMEOUT(ErrorCategory.EXECUTION, 504, "EXEC_003", "Task execution timed out", true),
    EXECUTION_FAILED(ErrorCategory.EXECUTION, 500, "EXEC_004", "Task execution failed", true),

    // ===== Plugin Errors =====
    PLUGIN_NOT_FOUND(ErrorCategory.PLUGIN, 404, "PLUGIN_001", "Plugin not found", false),
    PLUGIN_LOAD_FAILED(ErrorCategory.PLUGIN, 500, "PLUGIN_002", "Plugin load failed", true),
    PLUGIN_EXECUTION_FAILED(ErrorCategory.PLUGIN, 500, "PLUGIN_003", "Plugin execution failed", true),
    PLUGIN_UNSUPPORTED(ErrorCategory.PLUGIN, 400, "PLUGIN_004", "Unsupported plugin", false),

    // ===== MCP Errors =====
    MCP_TOOL_NOT_FOUND(ErrorCategory.MCP, 404, "MCP_001", "MCP tool not found", false),
    MCP_REQUEST_INVALID(ErrorCategory.MCP, 400, "MCP_002", "Invalid MCP request", false),
    MCP_RATE_LIMITED(ErrorCategory.MCP, 429, "MCP_003", "MCP rate limit exceeded", true),

    // ===== Tool Errors =====
    TOOL_NOT_FOUND(ErrorCategory.TOOL, 404, "TOOL_001", "Tool not found", false),
    TOOL_EXECUTION_FAILED(ErrorCategory.TOOL, 502, "TOOL_002", "Tool execution failed", true),

    // ===== Agent Errors =====
    AGENT_NOT_FOUND(ErrorCategory.AGENT, 404, "AGENT_001", "Agent not found", false),
    AGENT_STATE_INVALID(ErrorCategory.AGENT, 409, "AGENT_002", "Invalid agent state", false),
    AGENT_PLAN_FAILED(ErrorCategory.AGENT, 500, "AGENT_003", "Agent planning failed", true),

    // ===== Inference Errors =====
    INFERENCE_PROVIDER_UNAVAILABLE(ErrorCategory.INFERENCE, 503, "INF_001", "Inference provider unavailable", true),
    INFERENCE_REQUEST_FAILED(ErrorCategory.INFERENCE, 502, "INF_002", "Inference request failed", true),
    INFERENCE_RESPONSE_INVALID(ErrorCategory.INFERENCE, 502, "INF_003", "Inference response invalid", true),
    INFERENCE_RATE_LIMITED(ErrorCategory.INFERENCE, 429, "INF_004", "Inference rate limit exceeded", true),
    INFERENCE_AUTH_FAILED(ErrorCategory.INFERENCE, 401, "INF_005", "Inference authentication failed", false),

    // ===== Memory Errors =====
    MEMORY_NOT_FOUND(ErrorCategory.MEMORY, 404, "MEM_001", "Memory entry not found", false),
    MEMORY_STORE_FAILED(ErrorCategory.MEMORY, 500, "MEM_002", "Memory store failed", true),

    // ===== Vector Errors =====
    VECTOR_STORE_UNAVAILABLE(ErrorCategory.VECTOR, 503, "VEC_001", "Vector store unavailable", true),
    VECTOR_QUERY_FAILED(ErrorCategory.VECTOR, 502, "VEC_002", "Vector query failed", true),

    // ===== RAG Errors =====
    RAG_INDEX_NOT_FOUND(ErrorCategory.RAG, 404, "RAG_001", "RAG index not found", false),
    RAG_RETRIEVAL_FAILED(ErrorCategory.RAG, 502, "RAG_002", "RAG retrieval failed", true),

    // ===== Guardrail Errors =====
    GUARDRAIL_VIOLATION(ErrorCategory.GUARDRAIL, 403, "GRD_001", "Guardrail policy violated", false),
    GUARDRAIL_CONFIG_INVALID(ErrorCategory.GUARDRAIL, 500, "GRD_002", "Guardrail configuration invalid", false),

    // ===== HITL Errors =====
    HITL_TASK_NOT_FOUND(ErrorCategory.HITL, 404, "HITL_001", "HITL task not found", false),
    HITL_UNAUTHORIZED(ErrorCategory.HITL, 403, "HITL_002", "HITL access denied", false),

    // ===== EIP Errors =====
    EIP_ROUTE_NOT_FOUND(ErrorCategory.EIP, 404, "EIP_001", "EIP route not found", false),
    EIP_ROUTE_FAILED(ErrorCategory.EIP, 502, "EIP_002", "EIP route failed", true),

    // ===== Integration Errors =====
    INTEGRATION_TIMEOUT(ErrorCategory.INTEGRATION, 504, "INTEGRATION_001", "Integration timed out", true),
    INTEGRATION_FAILED(ErrorCategory.INTEGRATION, 502, "INTEGRATION_002", "Integration failed", true),

    // ===== Search Errors =====
    SEARCH_PROVIDER_UNAVAILABLE(ErrorCategory.SEARCH, 503, "SEARCH_001", "Search provider unavailable", true),

    // ===== Security Errors =====
    SECURITY_UNAUTHORIZED(ErrorCategory.SECURITY, 401, "SEC_001", "Unauthorized", false),
    SECURITY_FORBIDDEN(ErrorCategory.SECURITY, 403, "SEC_002", "Forbidden", false),
    SECURITY_SECRET_NOT_FOUND(ErrorCategory.SECURITY, 404, "SEC_003", "Secret not found", false),
    SECURITY_SECRET_BACKEND_UNAVAILABLE(ErrorCategory.SECURITY, 503, "SEC_004", "Secret backend unavailable", true),
    SECURITY_SECRET_ENCRYPTION_FAILED(ErrorCategory.SECURITY, 500, "SEC_005", "Secret encryption failed", false),
    SECURITY_SECRET_DECRYPTION_FAILED(ErrorCategory.SECURITY, 500, "SEC_006", "Secret decryption failed", false),
    SECURITY_SECRET_INVALID_PATH(ErrorCategory.SECURITY, 400, "SEC_007", "Secret path invalid", false),
    SECURITY_SECRET_QUOTA_EXCEEDED(ErrorCategory.SECURITY, 429, "SEC_008", "Secret quota exceeded", true),
    SECURITY_SECRET_VERSION_NOT_FOUND(ErrorCategory.SECURITY, 404, "SEC_009", "Secret version not found", false),
    SECURITY_SECRET_EXPIRED(ErrorCategory.SECURITY, 410, "SEC_010", "Secret expired", false),
    SECURITY_SECRET_ROTATION_FAILED(ErrorCategory.SECURITY, 500, "SEC_011", "Secret rotation failed", true),

    // ===== Tenant Errors =====
    TENANT_NOT_FOUND(ErrorCategory.TENANT, 404, "TENANT_001", "Tenant not found", false),
    TENANT_INVALID(ErrorCategory.TENANT, 400, "TENANT_002", "Invalid tenant", false),

    // ===== Configuration Errors =====
    CONFIG_MISSING(ErrorCategory.CONFIG, 500, "CONFIG_001", "Configuration missing", false),
    CONFIG_INVALID(ErrorCategory.CONFIG, 500, "CONFIG_002", "Configuration invalid", false),

    // ===== Validation Errors =====
    VALIDATION_FAILED(ErrorCategory.VALIDATION, 400, "VAL_001", "Validation failed", false),
    VALIDATION_MISSING_FIELD(ErrorCategory.VALIDATION, 400, "VAL_002", "Missing required field", false),

    // ===== Storage Errors =====
    STORAGE_READ_FAILED(ErrorCategory.STORAGE, 500, "STORAGE_001", "Storage read failed", true),
    STORAGE_WRITE_FAILED(ErrorCategory.STORAGE, 500, "STORAGE_002", "Storage write failed", true),
    STORAGE_CONFLICT(ErrorCategory.STORAGE, 409, "STORAGE_003", "Storage conflict", false),

    // ===== Network Errors =====
    NETWORK_ERROR(ErrorCategory.NETWORK, 502, "NET_001", "Network error", true),
    NETWORK_UNAVAILABLE(ErrorCategory.NETWORK, 503, "NET_002", "Network unavailable", true),

    // ===== Timeout Errors =====
    TIMEOUT(ErrorCategory.TIMEOUT, 504, "TIMEOUT_001", "Operation timed out", true),

    // ===== Rate Limit Errors =====
    RATE_LIMITED(ErrorCategory.RATE_LIMIT, 429, "RATE_001", "Rate limit exceeded", true),

    // ===== Concurrency Errors =====
    CONCURRENCY_CONFLICT(ErrorCategory.CONCURRENCY, 409, "CONC_001", "Concurrency conflict", true),
    CONCURRENCY_LIMIT(ErrorCategory.CONCURRENCY, 429, "CONC_002", "Concurrency limit exceeded", true),

    // ===== Runtime/Internal Errors =====
    RUNTIME_ERROR(ErrorCategory.RUNTIME, 500, "RUNTIME_001", "Runtime error", true),
    INTERNAL_ERROR(ErrorCategory.INTERNAL, 500, "INTERNAL_001", "Internal server error", true);

    public enum ErrorCategory {
        CORE("CORE"),
        ORCHESTRATION("ORCH"),
        EXECUTION("EXEC"),
        PLUGIN("PLUGIN"),
        MCP("MCP"),
        TOOL("TOOL"),
        AGENT("AGENT"),
        INFERENCE("INF"),
        MEMORY("MEM"),
        VECTOR("VEC"),
        RAG("RAG"),
        GUARDRAIL("GRD"),
        HITL("HITL"),
        EIP("EIP"),
        INTEGRATION("INTEGRATION"),
        SEARCH("SEARCH"),
        SECURITY("SEC"),
        TENANT("TENANT"),
        CONFIG("CONFIG"),
        VALIDATION("VAL"),
        STORAGE("STORAGE"),
        NETWORK("NET"),
        TIMEOUT("TIMEOUT"),
        RATE_LIMIT("RATE"),
        CONCURRENCY("CONC"),
        RUNTIME("RUNTIME"),
        INTERNAL("INTERNAL");

        private final String prefix;

        ErrorCategory(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    private static final Map<String, ErrorCode> BY_CODE;
    private static final Map<ErrorCategory, Map<String, ErrorCode>> BY_CATEGORY;

    static {
        Map<String, ErrorCode> byCode = new HashMap<>();
        Map<ErrorCategory, Map<String, ErrorCode>> byCategory = new EnumMap<>(ErrorCategory.class);

        for (ErrorCategory category : ErrorCategory.values()) {
            byCategory.put(category, new HashMap<>());
        }

        for (ErrorCode errorCode : values()) {
            if (!errorCode.code.startsWith(errorCode.category.getPrefix() + "_")) {
                throw new IllegalStateException(
                        "ErrorCode prefix mismatch: " + errorCode.name() + " -> " + errorCode.code);
            }
            ErrorCode previous = byCode.put(errorCode.code, errorCode);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate error code: " + errorCode.code + " (" + errorCode.name() + ", " + previous.name()
                                + ")");
            }
            byCategory.get(errorCode.category).put(errorCode.code, errorCode);
        }

        BY_CODE = Collections.unmodifiableMap(byCode);
        Map<ErrorCategory, Map<String, ErrorCode>> immutableByCategory = new EnumMap<>(ErrorCategory.class);
        for (Map.Entry<ErrorCategory, Map<String, ErrorCode>> entry : byCategory.entrySet()) {
            immutableByCategory.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        BY_CATEGORY = Collections.unmodifiableMap(immutableByCategory);
    }

    private final ErrorCategory category;
    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
    private final boolean retryable;

    ErrorCode(ErrorCategory category, int httpStatus, String code, String defaultMessage, boolean retryable) {
        this.category = Objects.requireNonNull(category, "category");
        this.httpStatus = httpStatus;
        this.code = Objects.requireNonNull(code, "code");
        this.defaultMessage = Objects.requireNonNull(defaultMessage, "defaultMessage");
        this.retryable = retryable;
    }

    public static ErrorCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return INTERNAL_ERROR;
        }
        ErrorCode errorCode = BY_CODE.get(code.trim());
        return errorCode != null ? errorCode : INTERNAL_ERROR;
    }

    public static Map<String, ErrorCode> byCategory(ErrorCategory category) {
        Map<String, ErrorCode> codes = BY_CATEGORY.get(category);
        return codes == null ? Collections.emptyMap() : codes;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
