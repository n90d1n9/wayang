package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.error.ErrorCode;
import tech.kayys.wayang.error.WayangException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class McpResourceFailuresTest {

    @Test
    void wayangMapsFailureWithCodeMessageAndCause() {
        RuntimeException cause = new RuntimeException("executor failure");

        Throwable mapped = McpResourceFailures.wayang(
                        ErrorCode.TOOL_EXECUTION_FAILED,
                        "Execution failed")
                .apply(cause);

        WayangException error = assertInstanceOf(WayangException.class, mapped);
        assertEquals(ErrorCode.TOOL_EXECUTION_FAILED, error.getErrorCode());
        assertEquals("Execution failed: executor failure", error.getMessage());
        assertSame(cause, error.getCause());
    }
}
