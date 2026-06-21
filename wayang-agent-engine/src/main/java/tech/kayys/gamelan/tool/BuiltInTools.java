package tech.kayys.gamelan.tool;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Aggregates all registered {@link ToolHandler} beans and generates the
 * tool documentation block injected into every system prompt.
 *
 * <h2>Multi-name tool fix</h2>
 * Tools like {@link tech.kayys.gamelan.tool.builtin.HttpTool} register
 * multiple names ({@code http_get}, {@code http_post}, {@code http_put},
 * {@code http_delete}) via {@link ToolHandler#toolNames()}, but
 * {@link ToolHandler#toolName()} returns only the primary name.
 *
 * <p>The previous catalogue used {@code ToolHandler::toolName} for sorting and
 * display, which produced only ONE entry per multi-name tool. The LLM would
 * see {@code http_get} in the catalogue but have no documentation for
 * {@code http_post} — causing it to either not use POST or invent the syntax.
 *
 * <p>Fixed: catalogue iterates {@code toolNames()} and documents all variants,
 * collapsing them into a single entry grouped under the primary name.
 *
 * <h2>Caching</h2>
 * The catalogue is built once in {@link PostConstruct} — tools are static
 * at runtime. {@code describeAll()} returns the cached string; never
 * rebuilds it.
 */
@ApplicationScoped
public class BuiltInTools {

    @Inject @Any Instance<ToolHandler> handlers;

    private String cachedCatalogue;
    private int    cachedCount;

    @PostConstruct
    void buildCatalogue() {
        List<ToolHandler> sorted = StreamSupport.stream(handlers.spliterator(), false)
                .sorted(Comparator.comparing(ToolHandler::toolName))
                .toList();

        cachedCount = sorted.size();

        cachedCatalogue = sorted.stream()
                .map(h -> {
                    StringBuilder sb = new StringBuilder();

                    // Show all supported names if the handler handles more than one
                    List<String> allNames = h.toolNames();
                    if (allNames.size() == 1) {
                        sb.append("`").append(h.toolName()).append("`");
                    } else {
                        sb.append("`")
                          .append(String.join("` | `", allNames))
                          .append("`");
                    }
                    sb.append(" — ").append(h.description()).append("\n");

                    if (!h.parameters().isEmpty()) {
                        sb.append("  Parameters:\n");
                        h.parameters().forEach(p ->
                                sb.append("    - ").append(p).append("\n"));
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    /** Returns the pre-built tool catalogue (cached, never null). */
    public String describeAll() {
        return cachedCatalogue != null ? cachedCatalogue : "(no tools registered)";
    }

    /** Number of registered tool handlers (not names — one handler may have many names). */
    public int toolCount() { return cachedCount; }
}
