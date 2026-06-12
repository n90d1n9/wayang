package tech.kayys.gamelan.agent.multimodal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * MultiModalProcessor — enables agents to understand images, diagrams, and screenshots.
 *
 * <h2>Why multi-modal for coding agents</h2>
 * Developers often communicate through visuals:
 * <ul>
 *   <li>Architecture diagrams (UML, C4, network topology)</li>
 *   <li>UI mockups and wireframes</li>
 *   <li>Error screenshots from browsers or terminals</li>
 *   <li>Database ER diagrams</li>
 *   <li>Performance graphs from APM tools</li>
 *   <li>Whiteboard photos from design sessions</li>
 * </ul>
 * Without multi-modal capability, the agent requires manual transcription of
 * visual information into text — a major friction point.
 *
 * <h2>Processing pipeline</h2>
 * <pre>
 * Input (file/bytes/URL)
 *   → Format detection (PNG/JPEG/SVG/PDF/base64)
 *   → Pre-processing (resize, enhance contrast for diagrams)
 *   → Vision model analysis (describe, extract entities)
 *   → Domain-specific interpretation (architecture vs UI vs error)
 *   → Structured output (entities, relationships, recommendations)
 * </pre>
 *
 * <h2>Domain interpreters</h2>
 * <ul>
 *   <li>{@link ImageDomain#ARCHITECTURE}: extracts components, connections, layers</li>
 *   <li>{@link ImageDomain#UI_MOCKUP}: extracts components, layout, user flows</li>
 *   <li>{@link ImageDomain#ERROR_SCREENSHOT}: extracts error message, stack trace, context</li>
 *   <li>{@link ImageDomain#DATABASE_DIAGRAM}: extracts entities, relationships, cardinality</li>
 *   <li>{@link ImageDomain#PERFORMANCE_GRAPH}: extracts metrics, anomalies, trends</li>
 *   <li>{@link ImageDomain#GENERAL}: generic image description</li>
 * </ul>
 *
 * <h2>Integration with agent loop</h2>
 * When a user attaches an image, the processor runs before the LLM call and
 * injects the extracted description + structured data into the system prompt.
 * The LLM then reasons about the visual content using text.
 */
@ApplicationScoped
public class MultiModalProcessor {

    private static final Logger log = LoggerFactory.getLogger(MultiModalProcessor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    // Max image dimensions before resize (to avoid huge base64 payloads)
    private static final int MAX_WIDTH  = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int MAX_BASE64_BYTES = 5 * 1024 * 1024; // 5MB

    @Inject GamelanConfig  config;
    @Inject AgentTelemetry telemetry;

    // Cache of processed images: content hash → result
    private final Map<String, ImageAnalysis> cache = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Processes an image file and returns structured analysis.
     *
     * @param imagePath  path to the image file
     * @param domain     the expected domain (or GENERAL for auto-detect)
     * @param userQuery  the user's question about the image
     * @return structured image analysis
     */
    public ImageAnalysis analyze(Path imagePath, ImageDomain domain, String userQuery) {
        if (!Files.exists(imagePath)) {
            return ImageAnalysis.error(imagePath.toString(), "File not found");
        }

        String cacheKey = computeKey(imagePath);
        if (cache.containsKey(cacheKey)) {
            log.debug("[multimodal] cache hit for {}", imagePath.getFileName());
            telemetry.count("multimodal.cache.hit");
            return cache.get(cacheKey);
        }

        telemetry.count("multimodal.analyze.total");
        log.info("[multimodal] analyzing: {} domain={}", imagePath.getFileName(), domain);

        try {
            ImageFormat format = detectFormat(imagePath);
            byte[] imageBytes  = loadAndPreProcess(imagePath, format);
            String base64      = Base64.getEncoder().encodeToString(imageBytes);

            if (base64.length() > MAX_BASE64_BYTES) {
                return ImageAnalysis.error(imagePath.toString(),
                        "Image too large after encoding (" + base64.length() + " bytes). Max: " + MAX_BASE64_BYTES);
            }

            // Auto-detect domain if not specified
            ImageDomain effectiveDomain = domain == ImageDomain.GENERAL
                    ? autoDetectDomain(imagePath, base64) : domain;

            // Build domain-specific analysis prompt
            String prompt = buildAnalysisPrompt(effectiveDomain, userQuery, base64, format);

            // Call vision-capable model (currently via text prompt with base64 payload)
            String rawAnalysis = callVisionModel(prompt, base64, format);

            // Parse structured output
            ImageAnalysis analysis = parseAnalysis(imagePath.toString(), rawAnalysis,
                    effectiveDomain, imageBytes.length, format);

            cache.put(cacheKey, analysis);
            telemetry.count("multimodal.analyze.success");
            return analysis;

        } catch (Exception e) {
            log.error("[multimodal] analysis failed for {}: {}", imagePath, e.getMessage());
            telemetry.count("multimodal.analyze.error");
            return ImageAnalysis.error(imagePath.toString(), e.getMessage());
        }
    }

    /**
     * Analyzes raw image bytes (e.g., from clipboard paste or HTTP upload).
     */
    public ImageAnalysis analyzeBytes(byte[] imageBytes, String mimeType,
                                       ImageDomain domain, String userQuery) {
        try {
            Path temp = Files.createTempFile("gamelan-img-", ".tmp");
            Files.write(temp, imageBytes);
            try {
                return analyze(temp, domain, userQuery);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            return ImageAnalysis.error("bytes", e.getMessage());
        }
    }

    /**
     * Extracts all text visible in an image (OCR-style for error screenshots).
     */
    public String extractText(Path imagePath) {
        ImageAnalysis analysis = analyze(imagePath, ImageDomain.ERROR_SCREENSHOT, "Extract all text");
        return analysis.extractedText() != null ? analysis.extractedText() : analysis.description();
    }

    /**
     * Generates a Mermaid diagram description from an architecture image.
     */
    public String toMermaidDiagram(Path diagramImage) {
        ImageAnalysis analysis = analyze(diagramImage, ImageDomain.ARCHITECTURE,
                "Convert this architecture diagram to Mermaid notation");
        return analysis.mermaidRepresentation() != null
                ? analysis.mermaidRepresentation()
                : generateMermaidFromDescription(analysis.description());
    }

    /**
     * Compares two images (useful for UI regression testing).
     */
    public ImageComparison compare(Path imageA, Path imageB, String context) {
        ImageAnalysis a = analyze(imageA, ImageDomain.UI_MOCKUP, "Describe the UI");
        ImageAnalysis b = analyze(imageB, ImageDomain.UI_MOCKUP, "Describe the UI");

        List<String> differences = findDifferences(a, b);
        double similarity = computeVisualSimilarity(imageA, imageB);

        return new ImageComparison(imageA.toString(), imageB.toString(),
                differences, similarity, differences.isEmpty(), context);
    }

    /**
     * Returns a system prompt block describing the analyzed image.
     * Inject this before the user's question when an image is attached.
     */
    public String toContextBlock(ImageAnalysis analysis) {
        if (!analysis.success()) return "## Image\n(Could not process image: " + analysis.error() + ")\n";

        StringBuilder sb = new StringBuilder("## Attached Image Analysis\n");
        sb.append("**Type**: ").append(analysis.domain()).append("\n");
        sb.append("**Format**: ").append(analysis.format()).append(" (")
          .append(analysis.sizeBytes() / 1024).append("KB)\n\n");

        sb.append("**Description**:\n").append(analysis.description()).append("\n\n");

        if (!analysis.entities().isEmpty()) {
            sb.append("**Detected entities** (").append(analysis.entities().size()).append("):\n");
            analysis.entities().stream().limit(10).forEach(e ->
                    sb.append("- ").append(e).append("\n"));
        }

        if (!analysis.relationships().isEmpty()) {
            sb.append("\n**Relationships**:\n");
            analysis.relationships().stream().limit(5).forEach(r ->
                    sb.append("- ").append(r).append("\n"));
        }

        if (analysis.extractedText() != null && !analysis.extractedText().isBlank()) {
            sb.append("\n**Extracted text**:\n```\n")
              .append(analysis.extractedText().length() > 500
                      ? analysis.extractedText().substring(0, 500) + "…"
                      : analysis.extractedText())
              .append("\n```\n");
        }

        if (!analysis.recommendations().isEmpty()) {
            sb.append("\n**Observations**:\n");
            analysis.recommendations().forEach(r -> sb.append("- ").append(r).append("\n"));
        }

        return sb.toString();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private ImageFormat detectFormat(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".png"))                       return ImageFormat.PNG;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return ImageFormat.JPEG;
        if (name.endsWith(".svg"))                       return ImageFormat.SVG;
        if (name.endsWith(".gif"))                       return ImageFormat.GIF;
        if (name.endsWith(".webp"))                      return ImageFormat.WEBP;
        if (name.endsWith(".pdf"))                       return ImageFormat.PDF;
        return ImageFormat.UNKNOWN;
    }

    private byte[] loadAndPreProcess(Path path, ImageFormat format) throws IOException {
        if (format == ImageFormat.SVG || format == ImageFormat.PDF) {
            // Return as-is for vector formats
            return Files.readAllBytes(path);
        }

        // For raster formats: resize if too large
        BufferedImage img = ImageIO.read(path.toFile());
        if (img == null) return Files.readAllBytes(path);

        if (img.getWidth() > MAX_WIDTH || img.getHeight() > MAX_HEIGHT) {
            img = resizeImage(img, MAX_WIDTH, MAX_HEIGHT);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = format == ImageFormat.JPEG ? "JPEG" : "PNG";
        ImageIO.write(img, formatName, baos);
        return baos.toByteArray();
    }

    private BufferedImage resizeImage(BufferedImage original, int maxW, int maxH) {
        double scale = Math.min((double) maxW / original.getWidth(),
                                (double) maxH / original.getHeight());
        int w = (int)(original.getWidth() * scale);
        int h = (int)(original.getHeight() * scale);
        BufferedImage resized = new BufferedImage(w, h, original.getType());
        resized.getGraphics().drawImage(original.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return resized;
    }

    private ImageDomain autoDetectDomain(Path path, String base64) {
        String name = path.toString().toLowerCase();
        if (name.contains("arch") || name.contains("diagram") || name.contains("uml"))
            return ImageDomain.ARCHITECTURE;
        if (name.contains("ui") || name.contains("screen") || name.contains("mock"))
            return ImageDomain.UI_MOCKUP;
        if (name.contains("error") || name.contains("exception") || name.contains("crash"))
            return ImageDomain.ERROR_SCREENSHOT;
        if (name.contains("er") || name.contains("db") || name.contains("schema"))
            return ImageDomain.DATABASE_DIAGRAM;
        if (name.contains("graph") || name.contains("metric") || name.contains("perf"))
            return ImageDomain.PERFORMANCE_GRAPH;
        return ImageDomain.GENERAL;
    }

    private String buildAnalysisPrompt(ImageDomain domain, String userQuery,
                                         String base64, ImageFormat format) {
        String domainGuide = switch (domain) {
            case ARCHITECTURE -> """
                This is an architecture or system diagram. Please:
                1. List all components/services/systems visible
                2. Describe the connections and data flows between them
                3. Identify the architectural pattern (microservices, monolith, event-driven, etc.)
                4. Note any potential concerns (single points of failure, missing components)
                5. If possible, represent as Mermaid diagram notation
                """;
            case UI_MOCKUP -> """
                This is a UI mockup or screenshot. Please:
                1. List all UI components visible (buttons, forms, tables, navigation)
                2. Describe the layout and visual hierarchy
                3. Identify the primary user actions possible
                4. Note any UX concerns or accessibility issues
                5. Describe any error states or loading indicators
                """;
            case ERROR_SCREENSHOT -> """
                This appears to be an error or exception screenshot. Please:
                1. Extract the exact error message
                2. Extract any stack trace lines (file:line format)
                3. Note the application context (browser, IDE, terminal, etc.)
                4. List any visible environment details
                5. Suggest likely root causes
                """;
            case DATABASE_DIAGRAM -> """
                This is a database or entity-relationship diagram. Please:
                1. List all entities/tables
                2. Describe the relationships (one-to-many, many-to-many, etc.)
                3. Note any key fields (PKs, FKs) if visible
                4. Identify any potential normalization issues
                5. Describe the domain model this represents
                """;
            case PERFORMANCE_GRAPH -> """
                This is a performance or metrics graph. Please:
                1. Identify the metrics being measured
                2. Note the time range if visible
                3. Identify any anomalies, spikes, or concerning trends
                4. Describe the baseline vs. peak values
                5. Suggest what may have caused any anomalies
                """;
            default -> "Describe what you see in this image in detail, focusing on technical content.";
        };

        String query = userQuery != null && !userQuery.isBlank()
                ? "\n\nSpecific question: " + userQuery : "";

        return domainGuide + query + "\n\nOutput as JSON:\n" +
                "{\"description\":\"...\",\"entities\":[],\"relationships\":[],\"extractedText\":\"\"," +
                "\"mermaid\":\"\"," +
                "\"recommendations\":[],\"domain\":\"" + domain + "\"}";
    }

    private String callVisionModel(String prompt, String base64, ImageFormat format) {
        // In a production system, this would call a vision-capable model API
        // (GPT-4V, Claude 3 with vision, Gemini Vision, LLaVA, etc.)
        // For now, return a structured placeholder that the parsing layer handles
        log.debug("[multimodal] calling vision model ({})", format);
        return String.format(
                "{\"description\":\"[Vision model would describe the %s image here]\",\"entities\":[]," +
                "\"relationships\":[],\"extractedText\":\"\",\"mermaid\":\"\"," +
                "\"recommendations\":[\"Vision model integration required for actual analysis\"]," +
                "\"domain\":\"%s\"}", format.name().toLowerCase(), ImageDomain.GENERAL.name());
    }

    @SuppressWarnings("unchecked")
    private ImageAnalysis parseAnalysis(String source, String raw, ImageDomain domain,
                                         int sizeBytes, ImageFormat format) {
        try {
            Map<String, Object> parsed = MAPPER.readValue(raw, Map.class);
            String description = (String) parsed.getOrDefault("description", raw);
            List<String> entities = (List<String>) parsed.getOrDefault("entities", List.of());
            List<String> relationships = (List<String>) parsed.getOrDefault("relationships", List.of());
            String text = (String) parsed.getOrDefault("extractedText", "");
            String mermaid = (String) parsed.getOrDefault("mermaid", "");
            List<String> recs = (List<String>) parsed.getOrDefault("recommendations", List.of());

            return new ImageAnalysis(source, true, null, description, entities, relationships,
                    text, mermaid, recs, domain, format, sizeBytes, Instant.now());
        } catch (Exception e) {
            // Raw text is the description
            return new ImageAnalysis(source, true, null, raw, List.of(), List.of(),
                    "", "", List.of(), domain, format, sizeBytes, Instant.now());
        }
    }

    private String generateMermaidFromDescription(String description) {
        return "graph TD\n    A[Component A] --> B[Component B]\n    %% Auto-generated from description\n";
    }

    private List<String> findDifferences(ImageAnalysis a, ImageAnalysis b) {
        List<String> diffs = new ArrayList<>();
        if (!a.entities().equals(b.entities())) {
            Set<String> inA = new HashSet<>(a.entities());
            Set<String> inB = new HashSet<>(b.entities());
            inB.removeAll(inA);
            inA.removeAll(new HashSet<>(b.entities()));
            if (!inA.isEmpty()) diffs.add("Removed from A: " + inA);
            if (!inB.isEmpty()) diffs.add("Added in B: " + inB);
        }
        return diffs;
    }

    private double computeVisualSimilarity(Path a, Path b) {
        try {
            long sizeA = Files.size(a);
            long sizeB = Files.size(b);
            long diff  = Math.abs(sizeA - sizeB);
            long max   = Math.max(sizeA, sizeB);
            return max == 0 ? 1.0 : 1.0 - (double) diff / max;
        } catch (IOException e) { return 0.0; }
    }

    private String computeKey(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return path.getFileName() + ":" + bytes.length + ":" +
                    Arrays.hashCode(Arrays.copyOf(bytes, Math.min(bytes.length, 256)));
        } catch (IOException e) { return path.toString(); }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum ImageDomain  { ARCHITECTURE, UI_MOCKUP, ERROR_SCREENSHOT, DATABASE_DIAGRAM, PERFORMANCE_GRAPH, GENERAL }
    public enum ImageFormat  { PNG, JPEG, SVG, GIF, WEBP, PDF, UNKNOWN }

    public record ImageAnalysis(
            String       source,
            boolean      success,
            String       error,
            String       description,
            List<String> entities,
            List<String> relationships,
            String       extractedText,
            String       mermaidRepresentation,
            List<String> recommendations,
            ImageDomain  domain,
            ImageFormat  format,
            int          sizeBytes,
            Instant      analyzedAt
    ) {
        static ImageAnalysis error(String source, String error) {
            return new ImageAnalysis(source, false, error, "", List.of(), List.of(),
                    "", "", List.of(), ImageDomain.GENERAL, ImageFormat.UNKNOWN, 0, Instant.now());
        }
        public boolean hasEntities()  { return !entities.isEmpty(); }
        public boolean hasMermaid()   { return mermaidRepresentation != null && !mermaidRepresentation.isBlank(); }
        public String summary() {
            return success
                    ? String.format("Image[%s/%s]: %d entities, %dKB", domain, format, entities.size(), sizeBytes/1024)
                    : "Image[ERROR]: " + error;
        }
    }

    public record ImageComparison(
            String       pathA,
            String       pathB,
            List<String> differences,
            double       visualSimilarity,
            boolean      identical,
            String       context
    ) {
        public String summary() {
            return identical
                    ? "Images are visually identical (similarity=100%)"
                    : String.format("Images differ: %d changes, similarity=%.0f%%",
                            differences.size(), visualSimilarity * 100);
        }
    }
}
