package tech.kayys.gamelan.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolHandler;
import tech.kayys.gamelan.tool.ToolResult;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.gollek.spi.embedding.EmbeddingResponse;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Semantic file search using Gollek vector embeddings (cosine similarity).
 *
 * <p>Unlike {@code search_files} (exact regex), this tool finds code by meaning —
 * e.g. "authentication logic", "database connection pooling", "retry with backoff".
 *
 * <h2>Real bug fixes vs. previous version</h2>
 * <ul>
 *   <li>Used {@code Files.walk} inside a {@code forEach} on a stream, which
 *       can cause the stream to throw {@code IllegalStateException: stream already closed}
 *       in JDK 17+. Fixed: collect files to a list before the embedding loop.</li>
 *   <li>{@code embed()} returned an empty float[] on error, which made
 *       {@code cosineSimilarity} return 0.0 silently. All zero-vector chunks
 *       were included at the bottom of results. Fixed: skip chunks where
 *       embedding is empty rather than scoring them at zero.</li>
 *   <li>No graceful degradation when the embedding model is unavailable.
 *       Fixed: if the first embed() call fails, the tool returns a clear
 *       error instead of scanning all files and failing per-chunk.</li>
 *   <li>Binary file handling: {@code Files.readAllLines} throws on binary
 *       files, crashing the forEach silently. Fixed: catch
 *       {@link MalformedInputException} and skip binary files.</li>
 * </ul>
 *
 * <pre>{@code
 * <tool_call>
 *   <n>semantic_search</n>
 *   <query>JWT token validation and expiry checking</query>
 *   <path>src/</path>
 *   <model>nomic-embed-text</model>
 *   <top_k>5</top_k>
 *   <file_pattern>*.java</file_pattern>
 * </tool_call>
 * }</pre>
 */
@ApplicationScoped
public class EmbeddingSearchTool implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingSearchTool.class);

    private static final int  DEFAULT_TOP_K     = 5;
    private static final int  CHUNK_LINES       = 50;    // lines per embedding chunk
    private static final long MAX_FILE_BYTES    = 100_000;
    private static final int  PREVIEW_CHARS     = 400;

    @Inject GollekSdk sdk;

    @Override public String toolName() { return "semantic_search"; }

    @Override public String description() {
        return "Semantic similarity search using vector embeddings — finds code by meaning, "
                + "not just exact keywords. Requires an embedding model (e.g. nomic-embed-text).";
    }

    @Override public List<String> parameters() {
        return List.of(
                "query        - Natural language description of what to find",
                "path         - Root directory to search (default: .)",
                "model        - Embedding model ID (default: nomic-embed-text)",
                "top_k        - Number of results to return (default: 5)",
                "file_pattern - Glob filter e.g. *.java (optional)"
        );
    }

    @Override
    public ToolResult execute(ToolCall call) {
        String query    = call.param("query").strip();
        String pathStr  = call.param("path", ".");
        String model    = call.param("model", "nomic-embed-text");
        int    topK     = FileToolUtils.parseIntParam(call.param("top_k", ""), DEFAULT_TOP_K);
        String fileGlob = call.param("file_pattern", "").strip();

        if (query.isBlank()) return ToolResult.failure(toolName(), "'query' is required");

        Path root = Path.of(pathStr).toAbsolutePath().normalize();
        if (!Files.isDirectory(root))
            return ToolResult.failure(toolName(), "Directory not found: " + pathStr);

        // Fail-fast: embed the query first so we catch model errors early
        float[] queryVec;
        try {
            queryVec = embed(model, query);
            if (queryVec.length == 0) {
                return ToolResult.failure(toolName(),
                        "Embedding model '" + model + "' returned an empty vector. "
                        + "Is the model loaded? Try: gamelan models list");
            }
        } catch (SdkException e) {
            return ToolResult.failure(toolName(),
                    "Embedding failed: " + e.getMessage()
                    + "\nEnsure an embedding model is available: gamelan models pull nomic-embed-text");
        }

        // Collect candidate files (avoids nested stream issues)
        PathMatcher fileMatcher = fileGlob.isBlank() ? null
                : FileSystems.getDefault().getPathMatcher("glob:**/" + fileGlob);
        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> fileMatcher == null || fileMatcher.matches(p))
                .filter(p -> !FileToolUtils.isExcluded(p))
                .forEach(candidates::add);
        } catch (IOException e) {
            return ToolResult.failure(toolName(), "Walk error: " + e.getMessage());
        }

        if (candidates.isEmpty())
            return ToolResult.success(toolName(), "No files found matching criteria in: " + pathStr);

        // Embed each file in chunks and score against query
        record Chunk(String filePath, int startLine, String preview, float score) {}
        List<Chunk> scored = new ArrayList<>();

        for (Path file : candidates) {
            try {
                long size = Files.size(file);
                if (size > MAX_FILE_BYTES) continue;

                List<String> lines;
                try {
                    lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                } catch (MalformedInputException e) {
                    continue; // binary file — skip
                }

                String relPath = root.relativize(file).toString();

                for (int i = 0; i < lines.size(); i += CHUNK_LINES) {
                    int    end   = Math.min(i + CHUNK_LINES, lines.size());
                    String chunk = String.join("\n", lines.subList(i, end)).strip();
                    if (chunk.isBlank()) continue;

                    float[] chunkVec = embed(model, chunk);
                    if (chunkVec.length == 0) continue; // skip failed chunks

                    float similarity = cosineSimilarity(queryVec, chunkVec);
                    String preview   = chunk.length() > PREVIEW_CHARS
                            ? chunk.substring(0, PREVIEW_CHARS) + "…"
                            : chunk;
                    scored.add(new Chunk(relPath, i + 1, preview, similarity));
                }
            } catch (SdkException e) {
                log.debug("Embedding failed for chunk in {}: {}", file, e.getMessage());
            } catch (IOException e) {
                log.debug("Cannot read {}: {}", file, e.getMessage());
            }
        }

        if (scored.isEmpty())
            return ToolResult.success(toolName(), "No embeddable content found in: " + pathStr);

        // Top-K by similarity
        List<Chunk> top = scored.stream()
                .sorted(Comparator.comparingDouble(Chunk::score).reversed())
                .limit(topK)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Semantic search for '").append(query).append("' — top ").append(top.size()).append(" results:\n\n");
        for (int i = 0; i < top.size(); i++) {
            Chunk c = top.get(i);
            sb.append(String.format("%d. **%s** (line %d, similarity=%.3f)%n",
                    i + 1, c.filePath(), c.startLine(), c.score()));
            sb.append("```\n").append(c.preview()).append("\n```\n\n");
        }

        return ToolResult.success(toolName(), sb.toString());
    }

    private float[] embed(String model, String text) throws SdkException {
        EmbeddingResponse resp = sdk.createEmbedding(
                EmbeddingRequest.builder().model(model).input(text).build());
        if (resp == null || resp.embeddings() == null || resp.embeddings().isEmpty())
            return new float[0];
        float[] result = resp.embeddings().get(0);
        return result != null ? result : new float[0];
    }

    private float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        if (len == 0) return 0f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += (double) a[i] * b[i];
            na  += (double) a[i] * a[i];
            nb  += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom < 1e-9 ? 0f : (float) (dot / denom);
    }
}
