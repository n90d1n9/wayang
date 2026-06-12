Legacy Gollek MCP bridge sources live here while the active module is rebuilt
around Wayang `agent-spi` contracts.

The legacy sources depended on old Gollek MCP client DTOs and context/result
skill APIs. Keep them out of Maven's active source tree until a concrete MCP
transport adapter is mapped to `McpToolClient`.
