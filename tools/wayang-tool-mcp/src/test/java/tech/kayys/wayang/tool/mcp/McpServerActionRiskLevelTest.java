package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;

import static tech.kayys.wayang.tool.mcp.McpServerActionTestFixtures.actionWithSeverity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class McpServerActionRiskLevelTest {

    @Test
    void normalizesRiskLevelValues() {
        assertEquals(McpServerActionRiskLevel.LOW, McpServerActionRiskLevel.normalize(" low "));
        assertEquals(McpServerActionRiskLevel.MEDIUM, McpServerActionRiskLevel.normalize("medium"));
        assertNull(McpServerActionRiskLevel.normalize(" "));
    }

    @Test
    void derivesRiskLevelFromSeverityAndAutomationSafety() {
        assertEquals(McpServerActionRiskLevel.HIGH,
                McpServerActionRiskLevel.from(actionWithSeverity(McpIssueSeverity.CRITICAL, true)));
        assertEquals(McpServerActionRiskLevel.MEDIUM,
                McpServerActionRiskLevel.from(actionWithSeverity(McpIssueSeverity.WARNING, true)));
        assertEquals(McpServerActionRiskLevel.LOW,
                McpServerActionRiskLevel.from(actionWithSeverity(McpIssueSeverity.INFO, true)));
        assertEquals(McpServerActionRiskLevel.MEDIUM,
                McpServerActionRiskLevel.from(actionWithSeverity(McpIssueSeverity.INFO, false)));
    }

    @Test
    void unknownSeverityHasUnknownRisk() {
        assertEquals(McpServerActionRiskLevel.UNKNOWN,
                McpServerActionRiskLevel.from(actionWithSeverity("custom", true)));
    }
}
