package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.action;
import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.preview;
import static org.junit.jupiter.api.Assertions.assertEquals;

class McpServerActionExecutionPolicyTest {

    @Test
    void allowsAutomatableRunSyncAction() {
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        true,
                        true)), supports(McpServerActionCatalog.ACTION_RUN_SYNC));

        assertEquals(true, decision.allowed());
        assertEquals(null, decision.rejectionReason());
    }

    @Test
    void rejectsActionWithoutStructuredOperation() {
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        true,
                        false)), supports(McpServerActionCatalog.ACTION_RUN_SYNC));

        assertEquals(false, decision.allowed());
        assertEquals(McpServerActionExecutionPolicy.REASON_NO_STRUCTURED_OPERATION,
                decision.rejectionReason());
    }

    @Test
    void rejectsActionThatRequiresReview() {
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        false,
                        true)), supports(McpServerActionCatalog.ACTION_RUN_SYNC));

        assertEquals(false, decision.allowed());
        assertEquals(McpServerActionExecutionPolicy.REASON_NOT_SAFE_TO_AUTOMATE,
                decision.rejectionReason());
    }

    @Test
    void rejectsUnsupportedActionCode() {
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview(action(
                        McpServerActionCatalog.ACTION_REVIEW_STALE_TOOLS,
                        true,
                        true)), supports(McpServerActionCatalog.ACTION_RUN_SYNC));

        assertEquals(false, decision.allowed());
        assertEquals(McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE,
                decision.rejectionReason());
    }

    @Test
    void rejectsWhenSupportPredicateIsMissing() {
        McpServerActionExecutionPolicy.Decision decision =
                McpServerActionExecutionPolicy.evaluate(preview(action(
                        McpServerActionCatalog.ACTION_RUN_SYNC,
                        true,
                        true)), null);

        assertEquals(false, decision.allowed());
        assertEquals(McpServerActionExecutionPolicy.REASON_UNSUPPORTED_ACTION_CODE,
                decision.rejectionReason());
    }

    private static Predicate<String> supports(String supportedActionCode) {
        return actionCode -> supportedActionCode.equals(McpServerActionIdentity.normalizeActionCode(actionCode));
    }
}
