package tech.kayys.wayang.tool.dto;

public record McpServerRegistryResponse(
        String name,
        String transport,
        String command,
        String url,
        String argsJson,
        String envJson,
        String source,
        String syncSchedule,
        boolean enabled) {
}
