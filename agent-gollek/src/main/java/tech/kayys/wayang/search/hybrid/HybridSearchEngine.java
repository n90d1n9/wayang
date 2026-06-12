package tech.kayys.gamelan.search.hybrid;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.cache.semantic.SemanticEmbeddingCache;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * HybridSearchEngine — fuses BM25 keyword search with semantic vector search.
 *
 * <h2>Why hybrid beats either alone</h2>
 * <pre>
 * BM25 keyword search:
 *   + Exact term matching (critical for variable names, error messages, APIs)
 *   + Fast, no embedding needed
 *   - Misses semantic synonyms ("authenticate" ≠ "login" in BM25)
 *   - Fails on paraphrase ("fix the bug" won't find "resolve the issue")
 *
 * Semantic vector search:
 *   + Finds conceptually related content regardless of exact wording
 *   + Handles paraphrases and synonyms
 *   - Poor at exact string matching (misses "NullPointerException" as a term)
 *   - Requires embedding model (latency + cost)
 *
 * Hybrid (Reciprocal Rank Fusion):
 *   + Gets exact matches AND semantic matches
 *   + Results from both lists reinforce each other
 *   + No single failure mode
 * </pre>
 *
 * <h2>Reciprocal Rank Fusion (RRF)</h2>
 * Final score = Σ(1 / (k + rank_in_list)) across all result lists.
 * k=60 is the standard constant that prevents high-rank results from
 * dominating when they appear in only one list.
 *
 * <h2>Learned re-ranking</h2>
 * Search results are re-ranked based on:
 * <ul>
 *   <li>File recency (recently modified files score higher)</li>
 *   <li>Click-through signal (files the agent read and then modified)</li>
 *   <li>Role context (security-related files score higher for CRITIC/VERIFIER)</li>
 *   <li>Project structure (main source files over test files for implementation tasks)</li>
 * </ul>
 */
@ApplicationScoped
public class HybridSearchEngine {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchEngine.class);
    private static final int RRF_K = 60;
    private static final int MAX_RESULTS = 50;
    private static final int BM25_K1 = 1_200;   // term frequency saturation (×1000 for int math)
    private static final int BM25_B  = 750;     // field length normalization (×1000)

    @Inject SemanticEmbeddingCache embedCache;
    @Inject AgentTelemetry         telemetry;

    // Click-through signal: file path → access count
    private final Map<String, AtomicInteger> clickSignal = new ConcurrentHashMap<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Searches the given root directory using hybrid BM25 + semantic search.
     *
     * @param query     the search query (natural language or keyword)
     * @param root      the directory to search
     * @param options   search configuration
     * @return ranked search results
     */
    public SearchResults search(String query, Path root, SearchOptions options) {
        long start = System.currentTimeMillis();
        telemetry.count("search.hybrid.total");

        // Run BM25 and semantic search concurrently
        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        Future<List<RankedResult>> bm25Future = exec.submit(() -> bm25Search(query, root, options));
        Future<List<RankedResult>> semanticFuture = exec.submit(() -> semanticSearch(query, root, options));
        exec.shutdown();

        List<RankedResult> bm25Results, semanticResults;
        try {
            bm25Results     = bm25Future.get(10, TimeUnit.SECONDS);
            semanticResults = semanticFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[hybrid-search] search timeout: {}", e.getMessage());
            bm25Results = List.of();
            semanticResults = List.of();
        }

        // Fuse via Reciprocal Rank Fusion
        List<RankedResult> fused = reciprocalRankFusion(bm25Results, semanticResults);

        // Apply learned re-ranking
        List<RankedResult> reranked = rerank(fused, query, options);

        long elapsedMs = System.currentTimeMillis() - start;
        telemetry.recordLatency("search.hybrid.latency", elapsedMs);

        log.debug("[hybrid-search] '{}': bm25={} semantic={} fused={} in {}ms",
                truncate(query, 50), bm25Results.size(), semanticResults.size(),
                reranked.size(), elapsedMs);

        return new SearchResults(query, reranked, bm25Results.size(),
                semanticResults.size(), elapsedMs);
    }

    /**
     * Records that a file was accessed (click-through signal for re-ranking).
     */
    public void recordAccess(String filePath) {
        clickSignal.computeIfAbsent(filePath, k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * Returns search statistics.
     */
    public SearchStats stats() {
        return new SearchStats(
                telemetry.getCount("search.hybrid.total"),
                (long) telemetry.histogram("search.hybrid.latency").avg(),
                telemetry.histogram("search.hybrid.latency").p95(),
                clickSignal.size());
    }

    // ── BM25 Search ────────────────────────────────────────────────────────

    private List<RankedResult> bm25Search(String query, Path root, SearchOptions opts) {
        List<String> queryTerms = tokenize(query.toLowerCase());
        if (queryTerms.isEmpty()) return List.of();

        List<FileDocument> docs = collectDocs(root, opts);
        if (docs.isEmpty()) return List.of();

        // Compute average document length
        double avgDocLen = docs.stream().mapToInt(FileDocument::termCount).average().orElse(1);

        // Compute IDF for each query term
        Map<String, Double> idf = computeIDF(queryTerms, docs);

        // Score each document
        return docs.stream()
                .map(doc -> {
                    double score = queryTerms.stream().mapToDouble(term -> {
                        int tf = doc.termFrequency(term);
                        if (tf == 0) return 0;
                        double idfScore = idf.getOrDefault(term, 0.0);
                        // BM25 formula: IDF × (tf × (k1+1)) / (tf + k1 × (1 - b + b × dl/avgdl))
                        double k1 = BM25_K1 / 1000.0;
                        double b  = BM25_B  / 1000.0;
                        double dl = doc.termCount();
                        double num = tf * (k1 + 1);
                        double den = tf + k1 * (1 - b + b * dl / avgDocLen);
                        return idfScore * num / den;
                    }).sum();
                    return new RankedResult(doc.path(), doc.snippet(queryTerms),
                            score, 0.0, SearchSource.BM25);
                })
                .filter(r -> r.bm25Score() > 0)
                .sorted(Comparator.comparingDouble(RankedResult::bm25Score).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    private Map<String, Double> computeIDF(List<String> terms, List<FileDocument> docs) {
        int N = docs.size();
        Map<String, Double> idf = new HashMap<>();
        for (String term : terms) {
            long df = docs.stream().filter(d -> d.termFrequency(term) > 0).count();
            idf.put(term, Math.log((N - df + 0.5) / (df + 0.5) + 1));
        }
        return idf;
    }

    // ── Semantic Search ────────────────────────────────────────────────────

    private List<RankedResult> semanticSearch(String query, Path root, SearchOptions opts) {
        float[] queryVec = embedCache.embed(query, SemanticEmbeddingCache.TtlType.FILE);
        if (queryVec.length == 0) return List.of();

        List<FileDocument> docs = collectDocs(root, opts);

        return docs.stream()
                .map(doc -> {
                    String sample = doc.content().length() > 2000
                            ? doc.content().substring(0, 2000) : doc.content();
                    float[] docVec = embedCache.embed(sample, SemanticEmbeddingCache.TtlType.FILE);
                    double sim = cosineSimilarity(queryVec, docVec);
                    return new RankedResult(doc.path(), doc.snippet(List.of()),
                            0.0, sim, SearchSource.SEMANTIC);
                })
                .filter(r -> r.semanticScore() > 0.3)
                .sorted(Comparator.comparingDouble(RankedResult::semanticScore).reversed())
                .limit(MAX_RESULTS)
                .toList();
    }

    // ── Reciprocal Rank Fusion ─────────────────────────────────────────────

    private List<RankedResult> reciprocalRankFusion(List<RankedResult> bm25,
                                                      List<RankedResult> semantic) {
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, RankedResult> byPath = new HashMap<>();

        // Score from BM25 list
        for (int i = 0; i < bm25.size(); i++) {
            RankedResult r = bm25.get(i);
            rrfScores.merge(r.path(), 1.0 / (RRF_K + i + 1), Double::sum);
            byPath.put(r.path(), r);
        }

        // Score from semantic list
        for (int i = 0; i < semantic.size(); i++) {
            RankedResult r = semantic.get(i);
            rrfScores.merge(r.path(), 1.0 / (RRF_K + i + 1), Double::sum);
            byPath.putIfAbsent(r.path(), r);
        }

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_RESULTS)
                .map(e -> {
                    RankedResult base = byPath.get(e.getKey());
                    return new RankedResult(e.getKey(), base.snippet(),
                            base.bm25Score(), base.semanticScore(), SearchSource.HYBRID,
                            e.getValue());
                })
                .toList();
    }

    // ── Re-ranking ────────────────────────────────────────────────────────

    private List<RankedResult> rerank(List<RankedResult> results, String query, SearchOptions opts) {
        return results.stream()
                .map(r -> {
                    double boost = 1.0;

                    // Click-through boost (up to 2×)
                    int clicks = clickSignal.getOrDefault(r.path(), new AtomicInteger()).get();
                    boost += Math.min(1.0, clicks * 0.1);

                    // Recency boost
                    try {
                        long daysOld = Files.exists(Path.of(r.path()))
                                ? java.time.Duration.between(
                                    Files.getLastModifiedTime(Path.of(r.path())).toInstant(),
                                    java.time.Instant.now()).toDays()
                                : 365;
                        boost += Math.max(0, 0.3 - daysOld * 0.01); // up to 0.3 for recent files
                    } catch (IOException ignored) {}

                    // Test file penalty for implementation queries
                    if (!opts.includeTests() && r.path().contains("Test")) boost *= 0.5;

                    // Security file boost for security queries
                    if (query.toLowerCase().contains("security") ||
                            query.toLowerCase().contains("auth")) {
                        String p = r.path().toLowerCase();
                        if (p.contains("security") || p.contains("auth") || p.contains("jwt")) {
                            boost *= 1.5;
                        }
                    }

                    return new RankedResult(r.path(), r.snippet(),
                            r.bm25Score(), r.semanticScore(),
                            r.source(), r.rrfScore() * boost);
                })
                .sorted(Comparator.comparingDouble(RankedResult::rrfScore).reversed())
                .toList();
    }

    // ── File collection ────────────────────────────────────────────────────

    private List<FileDocument> collectDocs(Path root, SearchOptions opts) {
        PathMatcher matcher = opts.globPattern() != null
                ? FileSystems.getDefault().getPathMatcher("glob:**/" + opts.globPattern())
                : null;
        try (Stream<Path> walk = Files.walk(root, opts.maxDepth())) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> isSearchable(p, opts))
                    .filter(p -> matcher == null || matcher.matches(p))
                    .limit(500)
                    .map(p -> loadDoc(root, p))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            log.warn("[hybrid-search] walk failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isSearchable(Path p, SearchOptions opts) {
        String s = p.toString();
        if (s.contains("/.git/") || s.contains("/target/") ||
                s.contains("/node_modules/") || s.contains("/.gradle/")) return false;
        if (!opts.includeTests() && s.contains("Test")) return false;
        try { return Files.size(p) < 200_000; } catch (IOException e) { return false; }
    }

    private FileDocument loadDoc(Path root, Path file) {
        try {
            String content = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
            return new FileDocument(root.relativize(file).toString(), content);
        } catch (Exception e) { return null; }
    }

    // ── Utility ────────────────────────────────────────────────────────────

    private List<String> tokenize(String text) {
        return Arrays.stream(text.split("[^a-zA-Z0-9_]+"))
                .filter(t -> t.length() >= 2)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0) return 0;
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += (double)a[i]*b[i]; na += (double)a[i]*a[i]; nb += (double)b[i]*b[i];
        }
        double d = Math.sqrt(na)*Math.sqrt(nb);
        return d < 1e-9 ? 0 : dot/d;
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0,max)+"…" : (s!=null?s:"");
    }

    // ── Inner types ────────────────────────────────────────────────────────

    private static final class FileDocument {
        private final String path;
        private final String content;
        private final Map<String, Integer> termFreqs = new HashMap<>();
        private final int termCount;

        FileDocument(String path, String content) {
            this.path    = path;
            this.content = content;
            // Pre-compute term frequencies
            Arrays.stream(content.toLowerCase().split("[^a-zA-Z0-9_]+"))
                    .filter(t -> t.length() >= 2)
                    .forEach(t -> termFreqs.merge(t, 1, Integer::sum));
            this.termCount = termFreqs.values().stream().mapToInt(i -> i).sum();
        }

        int termFrequency(String term) { return termFreqs.getOrDefault(term, 0); }
        int termCount() { return Math.max(1, termCount); }
        String path() { return path; }
        String content() { return content; }

        String snippet(List<String> queryTerms) {
            if (queryTerms.isEmpty()) {
                return content.length() > 200 ? content.substring(0, 200) + "…" : content;
            }
            // Find first match position
            String lower = content.toLowerCase();
            int pos = queryTerms.stream()
                    .mapToInt(t -> lower.indexOf(t))
                    .filter(i -> i >= 0).min().orElse(0);
            int start = Math.max(0, pos - 50);
            int end = Math.min(content.length(), pos + 150);
            String snippet = content.substring(start, end);
            return (start > 0 ? "…" : "") + snippet + (end < content.length() ? "…" : "");
        }
    }

    public enum SearchSource { BM25, SEMANTIC, HYBRID }

    public record RankedResult(
            String       path,
            String       snippet,
            double       bm25Score,
            double       semanticScore,
            SearchSource source,
            double       rrfScore
    ) {
        RankedResult(String p, String s, double b, double sem, SearchSource src) {
            this(p, s, b, sem, src, 0.0);
        }
    }

    public record SearchOptions(
            String  globPattern,
            int     maxDepth,
            boolean includeTests,
            int     maxResults
    ) {
        public static SearchOptions defaults() {
            return new SearchOptions(null, 10, true, 20);
        }
        public static SearchOptions codeOnly() {
            return new SearchOptions("*.{java,kt,py,go,rs,ts,js}", 10, false, 20);
        }
        public static SearchOptions withGlob(String glob) {
            return new SearchOptions(glob, 10, true, 20);
        }
    }

    public record SearchResults(
            String             query,
            List<RankedResult> results,
            int                bm25Count,
            int                semanticCount,
            long               elapsedMs
    ) {
        public boolean isEmpty() { return results.isEmpty(); }
        public int size()        { return results.size(); }

        public String summary() {
            return String.format("Search[%s]: %d results (bm25=%d semantic=%d) in %dms",
                    query.length()>30?query.substring(0,30)+"…":query,
                    results.size(), bm25Count, semanticCount, elapsedMs);
        }
    }

    public record SearchStats(
            long totalSearches,
            long avgLatencyMs,
            long p95LatencyMs,
            int  indexedFiles
    ) {}
}
