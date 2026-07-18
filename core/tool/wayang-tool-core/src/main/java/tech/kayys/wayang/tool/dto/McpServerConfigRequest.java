package tech.kayys.wayang.tool.dto;

import java.util.List;
import java.util.Map;

public record McpServerConfigRequest(
        String transport,
        String command,
        String url,
        List<String> args,
        Map<String, String> env,
        Boolean enabled,
        String source,
        String syncSchedule) {
}
