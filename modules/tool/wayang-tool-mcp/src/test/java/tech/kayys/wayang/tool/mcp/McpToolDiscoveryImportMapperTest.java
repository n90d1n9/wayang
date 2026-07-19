package tech.kayys.wayang.tool.mcp;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.CapabilityLevel;
import tech.kayys.wayang.tool.dto.HttpMethod;
import tech.kayys.wayang.tool.dto.ToolType;
import tech.kayys.wayang.tool.entity.HttpExecutionConfig;
import tech.kayys.wayang.tool.entity.ToolGuardrails;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.discoveredTool;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.importRequest;
import static tech.kayys.wayang.tool.mcp.McpToolDiscoveryImportTestFixtures.readOnlyDiscoveredTool;

class McpToolDiscoveryImportMapperTest {

    @Test
    void mapsDiscoveredToolIntoRegistryEntity() {
        McpToolDiscoveryImportRequest request = importRequest(
                "docs",
                "http://docs.local/mcp",
                "knowledge",
                Map.of(
                        McpHttpJsonRpcClient.CONTEXT_MCP_HEADERS,
                        Map.of("Authorization", "Bearer token"),
                        McpHttpJsonRpcClient.CONTEXT_TIMEOUT_MS,
                        1500));
        McpDiscoveredTool discoveredTool = readOnlyDiscoveredTool(
                "docs",
                "search",
                "Search Docs",
                "Search documentation");

        tech.kayys.wayang.tool.entity.McpTool entity = McpToolDiscoveryImportMapper.toEntity(
                "tenant-1",
                request,
                "knowledge",
                discoveredTool);

        assertEquals("tenant-1", entity.getRequestId());
        assertEquals("knowledge", entity.getNamespace());
        assertEquals("knowledge:search", entity.getToolId());
        assertEquals("search", entity.getName());
        assertEquals("Search documentation", entity.getDescription());
        assertEquals(CapabilityLevel.READ_ONLY, entity.getCapabilityLevel());
        assertTrue(entity.isReadOnly());
        assertFalse(entity.isRequiresApproval());
        assertTrue(entity.getCapabilities().contains(McpToolLifecycle.READ_CAPABILITY));
        assertTrue(entity.getCapabilities().contains(McpToolLifecycle.serverCapability("docs")));
        assertTrue(entity.getTags().contains("knowledge"));
        assertEquals(HttpMethod.POST, entity.getExecutionConfig().getMethod());
        assertEquals("http://docs.local/mcp", entity.getExecutionConfig().getBaseUrl());
        assertEquals("", entity.getExecutionConfig().getPath());
        assertEquals("application/json", entity.getExecutionConfig().getContentType());
        assertEquals("application/json, text/event-stream", entity.getExecutionConfig().getAccept());
        assertEquals(Map.of("Authorization", "Bearer token"), entity.getExecutionConfig().getHeaders());
        assertEquals(1500, entity.getExecutionConfig().getTimeoutMs());
        assertEquals("search", entity.getOperationId());
        assertNotNull(entity.getGuardrails());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void refreshesDiscoveryOwnedFieldsOnExistingEntity() {
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant previousUpdatedAt = Instant.parse("2025-01-02T00:00:00Z");
        tech.kayys.wayang.tool.entity.McpTool existing = new tech.kayys.wayang.tool.entity.McpTool();
        existing.setToolId("knowledge:search");
        existing.setRequestId("tenant-1");
        existing.setNamespace("knowledge");
        existing.setCreatedAt(createdAt);
        existing.setCreatedBy("creator-1");
        existing.setUpdatedAt(previousUpdatedAt);
        existing.setAuthProfileId("auth-1");
        existing.setName("old-search");
        existing.setDescription("Old description");
        existing.setEnabled(false);
        existing.setReadOnly(false);
        existing.setRequiresApproval(true);
        existing.setTags(Set.of("old"));
        existing.setCapabilities(Set.of("old-capability"));

        HttpExecutionConfig executionConfig = new HttpExecutionConfig();
        executionConfig.setMethod(HttpMethod.POST);
        executionConfig.setBaseUrl("http://fresh.local/mcp");
        ToolGuardrails guardrails = new ToolGuardrails();
        guardrails.setRequireApproval(true);

        tech.kayys.wayang.tool.entity.McpTool source = new tech.kayys.wayang.tool.entity.McpTool();
        source.setToolId("ignored:search");
        source.setRequestId("tenant-2");
        source.setNamespace("ignored");
        source.setCreatedAt(Instant.parse("2025-02-01T00:00:00Z"));
        source.setCreatedBy("creator-2");
        source.setName("search");
        source.setDescription("Fresh description");
        source.setToolType(ToolType.CUSTOM);
        source.setCapabilityLevel(CapabilityLevel.READ_ONLY);
        source.setInputSchema(Map.<String, Object>of("type", "object"));
        source.setOutputSchema(Map.<String, Object>of("type", "object", "shape", "result"));
        source.setExecutionConfig(executionConfig);
        source.setGuardrails(guardrails);
        source.setEnabled(true);
        source.setReadOnly(true);
        source.setRequiresApproval(false);
        source.setTags(Set.of("knowledge", "mcp-imported"));
        source.setCapabilities(Set.of(McpToolLifecycle.READ_CAPABILITY));
        source.setOperationId("search");

        tech.kayys.wayang.tool.entity.McpTool updated = McpToolDiscoveryImportMapper.updateExisting(existing, source);

        assertSame(existing, updated);
        assertEquals("knowledge:search", updated.getToolId());
        assertEquals("tenant-1", updated.getRequestId());
        assertEquals("knowledge", updated.getNamespace());
        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals("creator-1", updated.getCreatedBy());
        assertEquals("auth-1", updated.getAuthProfileId());
        assertEquals("search", updated.getName());
        assertEquals("Fresh description", updated.getDescription());
        assertEquals(ToolType.CUSTOM, updated.getToolType());
        assertEquals(CapabilityLevel.READ_ONLY, updated.getCapabilityLevel());
        assertEquals(Map.of("type", "object"), updated.getInputSchema());
        assertEquals(Map.of("type", "object", "shape", "result"), updated.getOutputSchema());
        assertSame(executionConfig, updated.getExecutionConfig());
        assertSame(guardrails, updated.getGuardrails());
        assertTrue(updated.isEnabled());
        assertTrue(updated.isReadOnly());
        assertFalse(updated.isRequiresApproval());
        assertEquals(Set.of("knowledge", "mcp-imported"), updated.getTags());
        assertEquals(Set.of(McpToolLifecycle.READ_CAPABILITY), updated.getCapabilities());
        assertEquals("search", updated.getOperationId());
        assertTrue(updated.getUpdatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void resolvesImportEndpointAndDescriptionFallbacks() {
        McpToolDiscoveryImportRequest request = importRequest(
                "docs",
                null,
                null,
                Map.of(McpHttpJsonRpcClient.CONTEXT_MCP_ENDPOINT, "http://context.local/mcp"));
        McpDiscoveredTool titledTool = discoveredTool(
                "docs",
                "lookup",
                "Lookup Docs",
                null,
                Map.of());
        McpDiscoveredTool unnamedTool = discoveredTool("docs", "unknown");

        assertEquals("http://context.local/mcp", McpToolDiscoveryImportMapper.endpoint(request));
        assertEquals("docs:lookup", McpToolDiscoveryImportMapper.toolId("docs", "lookup"));
        assertEquals("Lookup Docs", McpToolDiscoveryImportMapper.description(titledTool));
        assertEquals("MCP tool unknown", McpToolDiscoveryImportMapper.description(unnamedTool));
    }
}
