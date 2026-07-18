package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.error.ErrorCode;
import tech.kayys.wayang.error.WayangException;

import java.util.function.Function;

final class McpResourceFailures {

    private McpResourceFailures() {
    }

    static Function<Throwable, Throwable> wayang(
            ErrorCode errorCode,
            String messagePrefix) {
        return throwable -> new WayangException(
                errorCode,
                messagePrefix + ": " + throwable.getMessage(),
                throwable);
    }
}
