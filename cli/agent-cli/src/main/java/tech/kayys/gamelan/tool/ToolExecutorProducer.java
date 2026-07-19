package tech.kayys.gamelan.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.ConfigProvider;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * CDI producer for {@link ToolExecutor}.
 * 
 * <p>Creates a ToolExecutor with default configuration:
 * <ul>
 *   <li>Approval mode: auto (no approval needed for known tools)</li>
 *   <li>Sandbox: disabled by default</li>
 *   <li>Working directory: current directory</li>
 * </ul>
 */
@ApplicationScoped
public class ToolExecutorProducer {

    @Produces
    @ApplicationScoped
    public ToolExecutor produceToolExecutor() {
        var config = ConfigProvider.getConfig();
        
        String approvalMode = config.getOptionalValue("gamelan.tools.approval-mode", String.class).orElse("auto");
        boolean sandboxEnabled = config.getOptionalValue("gamelan.tools.sandbox-enabled", Boolean.class).orElse(false);
        String trustedToolsConfig = config.getOptionalValue("gamelan.tools.trusted", String.class).orElse("");
        String blockedToolsConfig = config.getOptionalValue("gamelan.tools.blocked", String.class).orElse("");
        
        Set<String> trustedTools = parseToolsList(trustedToolsConfig);
        Set<String> blockedTools = parseToolsList(blockedToolsConfig);
        
        // Add some default trusted tools
        trustedTools.add("read_file");
        trustedTools.add("list_dir");
        trustedTools.add("search_files");
        trustedTools.add("grep");
        trustedTools.add("think");

        return new ToolExecutor(
                trustedTools,
                blockedTools,
                approvalMode,
                sandboxEnabled,
                Path.of(System.getProperty("user.dir"))
        );
    }

    private Set<String> parseToolsList(String config) {
        Set<String> tools = new HashSet<>();
        if (config != null && !config.isBlank()) {
            String[] parts = config.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    tools.add(trimmed);
                }
            }
        }
        return tools;
    }
}
