package tech.kayys.wayang.agent.api;

/**
 * Stable error payload for API resources.
 */
public record ApiErrorResponse(String error) {

    public ApiErrorResponse {
        error = error == null || error.isBlank() ? "Unknown error" : error;
    }

    public static ApiErrorResponse from(Throwable error) {
        if (error == null) {
            return new ApiErrorResponse("Unknown error");
        }
        return new ApiErrorResponse(error.getMessage() == null
                ? error.getClass().getSimpleName()
                : error.getMessage());
    }
}
