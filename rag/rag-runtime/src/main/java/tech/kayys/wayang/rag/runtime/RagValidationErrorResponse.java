package tech.kayys.wayang.rag.runtime;

public record RagValidationErrorResponse(
                String code,
                String field,
                String tenantId,
                String value,
                String message) {
}
