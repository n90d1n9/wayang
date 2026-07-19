package tech.kayys.wayang.tool.os;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Searches the web using Wikipedia or DuckDuckGo.
 *
 * <p>Set {@code -Dwayang.web_search.provider=wikipedia} (default) or {@code duckduckgo}.
 */
public final class WebSearchTool implements Tool {

    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int READ_TIMEOUT    = 15_000;

    @Override public String id()   { return "web_search"; }
    @Override public String name() { return "web_search"; }

    @Override
    public String description() {
        return "Search the internet (via Wikipedia or DuckDuckGo) and return relevant results. " +
               "Returns an instant answer (if available), an abstract/summary, and a list of " +
               "result snippets with titles and URLs. " +
               "Use 'num_results' to control how many results are returned (default 10, max 20). " +
               "Combine with the 'fetch_url' tool to read the full content of any result page.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Schema.object(Schema.props(
                "query",       Schema.string("The search query string."),
                "num_results", Schema.integer("Maximum number of results to return (1–20, default 10).")
        ), "query");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext context) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("'query' parameter is required.");
        }
        int numResults = 10;
        if (params.containsKey("num_results")) {
            numResults = Math.min(20, Math.max(1, ((Number) params.get("num_results")).intValue()));
        }

        String providerName = System.getProperty("wayang.web_search.provider", "duckduckgo").toLowerCase();
        SearchProvider provider = switch (providerName) {
            case "duckduckgo", "ddg" -> new DuckDuckGoProvider();
            case "google" -> new GoogleHtmlSearchProvider();
            case "wikipedia" -> new WikipediaProvider();
            default -> new DuckDuckGoProvider();
        };

        try {
            ToolResult result = provider.search(query, numResults);
            if (result.error() != null && result.error().contains("Telkomsel")) {
                // Fallback to Google if DuckDuckGo is blocked
                return new GoogleHtmlSearchProvider().search(query, numResults);
            }
            return result;
        } catch (Exception e) {
            return ToolResult.error("Web search failed: " + e.getMessage());
        }
    }

    public interface SearchProvider {
        ToolResult search(String query, int numResults) throws Exception;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wikipedia Provider
    // ─────────────────────────────────────────────────────────────────────────
    public static class WikipediaProvider implements SearchProvider {
        private static final String API_URL = "https://en.wikipedia.org/w/api.php?action=opensearch&namespace=0&format=json&search=";
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public ToolResult search(String query, int numResults) throws Exception {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_URL + encoded + "&limit=" + numResults;
            
            String json = fetchTextWithSslFallback(url, "application/json");
            JsonNode root = MAPPER.readTree(json);
            
            if (!root.isArray() || root.size() < 4) {
                return ToolResult.error("Unexpected Wikipedia API response format");
            }
            
            JsonNode titles = root.get(1);
            JsonNode summaries = root.get(2);
            JsonNode urls = root.get(3);
            
            if (titles.isEmpty()) {
                return ToolResult.success("No results found on Wikipedia. Try a different or more specific query.\n");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("=".repeat(60)).append("\n\n");
            sb.append("🔍 Wikipedia Results (").append(titles.size()).append("):\n\n");

            for (int i = 0; i < titles.size(); i++) {
                sb.append(String.format("%2d. %s%n", i + 1, titles.get(i).asText()));
                String summary = summaries.get(i).asText();
                if (!summary.isEmpty()) {
                    sb.append("    Snippet: ").append(summary).append("\n");
                }
                sb.append("    URL: ").append(urls.get(i).asText()).append("\n\n");
            }
            
            return ToolResult.success(sb.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DuckDuckGo Provider
    // ─────────────────────────────────────────────────────────────────────────
    public static class DuckDuckGoProvider implements SearchProvider {
        private static final String DDG_JSON_API  = "https://api.duckduckgo.com/";
        private static final String DDG_HTML_URL  = "https://html.duckduckgo.com/html/";

        @Override
        public ToolResult search(String query, int numResults) throws Exception {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Strategy 1: JSON API
            try {
                String url = DDG_JSON_API + "?q=" + encoded + "&format=json&no_html=1&skip_disambig=1";
                String json = fetchTextWithSslFallback(url, "application/json");
                ToolResult r = parseJson(query, json, numResults);
                if (!r.output().map(s -> s.contains("No results found")).orElse(true)) return r;
            } catch (Exception jsonEx) {
                // Fall through to HTML strategy
            }

            // Strategy 2: HTML endpoint
            try {
                String url = DDG_HTML_URL + "?q=" + encoded + "&kl=us-en";
                String html = fetchTextWithSslFallback(url, "text/html");
                if (html.contains("internetbaik") || html.contains("Internet Positif") || html.contains("Telkomsel")) {
                    return ToolResult.error("DuckDuckGo is blocked by your ISP (Telkomsel Internet Positif). Web search is unavailable without a VPN.");
                }
                return parseHtml(query, html, numResults);
            } catch (java.net.UnknownHostException e) {
                return ToolResult.error("No internet access or DNS resolution failed: " + e.getMessage());
            }
        }

        private static ToolResult parseJson(String query, String json, int numResults) {
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("=".repeat(60)).append("\n\n");

            String instantAnswer = extractJsonString(json, "Answer");
            if (!instantAnswer.isEmpty()) {
                sb.append("⚡ Instant Answer:\n").append(instantAnswer).append("\n\n");
            }

            String abstractText = extractJsonString(json, "AbstractText");
            String abstractURL  = extractJsonString(json, "AbstractURL");
            if (!abstractText.isEmpty()) {
                sb.append("📄 Summary:\n").append(abstractText).append("\n");
                if (!abstractURL.isEmpty()) sb.append("   Source: ").append(abstractURL).append("\n");
                sb.append("\n");
            }

            String definition    = extractJsonString(json, "Definition");
            String definitionURL = extractJsonString(json, "DefinitionURL");
            if (!definition.isEmpty()) {
                sb.append("📖 Definition:\n").append(definition).append("\n");
                if (!definitionURL.isEmpty()) sb.append("   Source: ").append(definitionURL).append("\n");
                sb.append("\n");
            }

            List<String[]> topics = extractRelatedTopics(json, numResults);
            if (!topics.isEmpty()) {
                sb.append("🔗 Related Topics (").append(topics.size()).append("):\n");
                for (int i = 0; i < topics.size(); i++) {
                    String[] t = topics.get(i);
                    sb.append(String.format("%2d. %s%n", i + 1, t[0]));
                    if (t[1] != null && !t[1].isEmpty()) sb.append("    URL: ").append(t[1]).append("\n");
                    sb.append("\n");
                }
            }

            if (instantAnswer.isEmpty() && abstractText.isEmpty() && definition.isEmpty() && topics.isEmpty()) {
                return ToolResult.success("No results found. Try a different or more specific query.\n");
            }
            return ToolResult.success(sb.toString());
        }

        private static ToolResult parseHtml(String query, String html, int numResults) {
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("=".repeat(60)).append("\n\n");

            List<String[]> results = extractHtmlResults(html, numResults);
            if (results.isEmpty()) {
                return ToolResult.success("No results found. Try a different or more specific query.\n");
            }

            sb.append("🔍 Web Results (").append(results.size()).append("):\n\n");
            for (int i = 0; i < results.size(); i++) {
                String[] r = results.get(i);
                sb.append(String.format("%2d. %s%n", i + 1, r[0]));      // title
                if (r[1] != null && !r[1].isEmpty()) {
                    sb.append("    Snippet: ").append(r[1]).append("\n"); // snippet
                }
                if (r[2] != null && !r[2].isEmpty()) {
                    sb.append("    URL: ").append(r[2]).append("\n");     // url
                }
                sb.append("\n");
            }
            return ToolResult.success(sb.toString());
        }

        private static List<String[]> extractHtmlResults(String html, int max) {
            List<String[]> results = new ArrayList<>();
            String[] markers = {"result__body", "result--web"};
            String marker = html.contains("result__body") ? "result__body" : "result--web";

            int pos = 0;
            while (results.size() < max) {
                int blockStart = html.indexOf(marker, pos);
                if (blockStart < 0) break;

                int blockEnd = html.indexOf("</div>", blockStart + marker.length());
                if (blockEnd < 0) break;
                int snippetEnd = html.indexOf("</div>", blockEnd + 1);
                if (snippetEnd > 0 && snippetEnd - blockStart < 2000) blockEnd = snippetEnd;

                String block = html.substring(blockStart, Math.min(blockEnd + 6, html.length()));

                String title   = extractHtmlTag(block, "result__title", "result__a");
                String snippet = extractHtmlTag(block, "result__snippet", "result__description");
                String url     = extractHref(block);

                title   = decodeEntities(stripTags(title)).strip();
                snippet = decodeEntities(stripTags(snippet)).strip();
                url     = cleanDdgRedirect(url);

                if (!title.isEmpty() || !url.isEmpty()) {
                    results.add(new String[]{title, snippet, url});
                }
                pos = blockStart + marker.length();
            }
            return results;
        }

        private static String extractHtmlTag(String block, String... classNames) {
            for (String cls : classNames) {
                int idx = block.indexOf("class=\"" + cls + "\"");
                if (idx < 0) idx = block.indexOf("class=\"" + cls + " ");
                if (idx < 0) continue;
                int tagOpen = block.lastIndexOf("<", idx);
                if (tagOpen < 0) continue;
                int tagClose = block.indexOf(">", idx);
                if (tagClose < 0) continue;
                int endIdx = block.indexOf("<", tagClose + 1);
                if (endIdx < 0) endIdx = block.length();
                return block.substring(tagClose + 1, endIdx);
            }
            return "";
        }

        private static String extractHref(String block) {
            int idx = block.indexOf("href=\"");
            if (idx < 0) return "";
            int start = idx + 6;
            int end = block.indexOf("\"", start);
            return end > start ? block.substring(start, end) : "";
        }

        private static String cleanDdgRedirect(String url) {
            if (url == null) return "";
            int uddg = url.indexOf("uddg=");
            if (uddg >= 0) {
                String encoded = url.substring(uddg + 5);
                int amp = encoded.indexOf('&');
                if (amp > 0) encoded = encoded.substring(0, amp);
                try {
                    return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                } catch (Exception ignore) {}
            }
            return url;
        }

        private static String stripTags(String s) {
            return s == null ? "" : s.replaceAll("<[^>]+>", "");
        }

        private static String decodeEntities(String s) {
            return s == null ? "" : s
                    .replace("&amp;",  "&").replace("&lt;",   "<").replace("&gt;",  ">")
                    .replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ")
                    .replace("&#39;",  "'").replaceAll("\\s+", " ");
        }

        private static String extractJsonString(String json, String key) {
            String searchKey = "\"" + key + "\":\"";
            int idx = json.indexOf(searchKey);
            if (idx < 0) return "";
            int start = idx + searchKey.length();
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"') break;
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(++i);
                    switch (next) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default  -> sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private static List<String[]> extractRelatedTopics(String json, int max) {
            List<String[]> results = new ArrayList<>();
            int topicsStart = json.indexOf("\"RelatedTopics\":[");
            if (topicsStart < 0) return results;
            topicsStart += "\"RelatedTopics\":[".length();

            int depth = 0, objStart = -1;
            for (int i = topicsStart; i < json.length() && results.size() < max; i++) {
                char c = json.charAt(i);
                if      (c == '{') { if (depth++ == 0) objStart = i; }
                else if (c == '}') {
                    if (--depth == 0 && objStart >= 0) {
                        String obj  = json.substring(objStart, i + 1);
                        String text = extractJsonString(obj, "Text");
                        String url  = extractJsonString(obj, "FirstURL");
                        if (!text.isEmpty()) results.add(new String[]{text, url});
                        objStart = -1;
                    }
                } else if (c == ']' && depth == 0) break;
            }
            return results;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Google Provider (Scraping)
    // ─────────────────────────────────────────────────────────────────────────
    public static class GoogleHtmlSearchProvider implements SearchProvider {
        private static final String GOOGLE_HTML_URL = "https://www.google.com/search";

        @Override
        public ToolResult search(String query, int numResults) throws Exception {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = GOOGLE_HTML_URL + "?q=" + encoded + "&num=" + numResults;
            String html;
            try {
                html = fetchTextWithSslFallback(url, "text/html");
            } catch (Exception e) {
                return ToolResult.error("Google search failed: " + e.getMessage());
            }

            if (html.contains("internetbaik") || html.contains("Internet Positif") || html.contains("Telkomsel")) {
                return ToolResult.error("Google is blocked by your ISP (Telkomsel Internet Positif).");
            }
            
            return parseHtml(query, html, numResults);
        }

        private static ToolResult parseHtml(String query, String html, int numResults) {
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("=".repeat(60)).append("\n\n");

            List<String[]> results = extractGoogleResults(html, numResults);
            if (results.isEmpty()) {
                return ToolResult.success("No results found on Google. Try a different query.\n");
            }

            sb.append("🔍 Web Results (").append(results.size()).append("):\n\n");
            for (int i = 0; i < results.size(); i++) {
                String[] r = results.get(i);
                sb.append(String.format("%2d. %s%n", i + 1, r[0]));      // title
                if (r[1] != null && !r[1].isEmpty()) {
                    sb.append("    Snippet: ").append(r[1]).append("\n"); // snippet
                }
                if (r[2] != null && !r[2].isEmpty()) {
                    sb.append("    URL: ").append(r[2]).append("\n");     // url
                }
                sb.append("\n");
            }
            return ToolResult.success(sb.toString());
        }

        private static List<String[]> extractGoogleResults(String html, int max) {
            List<String[]> results = new ArrayList<>();
            // Google's mobile/basic HTML fallback usually formats links like:
            // <a href="/url?q=https://example.com/...">
            int pos = 0;
            while (results.size() < max) {
                int linkStart = html.indexOf("<a href=\"/url?q=", pos);
                if (linkStart < 0) break;
                
                int urlStart = linkStart + 16;
                int urlEnd = html.indexOf("&amp;", urlStart);
                if (urlEnd < 0) break;
                
                String url = html.substring(urlStart, urlEnd);
                try {
                    url = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8);
                } catch (Exception ignore) {}

                int divTitleStart = html.indexOf("<h3", linkStart);
                int titleStart = html.indexOf(">", divTitleStart) + 1;
                int titleEnd = html.indexOf("</h3>", titleStart);
                
                String title = "";
                if (divTitleStart > 0 && divTitleStart < titleEnd) {
                    title = html.substring(titleStart, titleEnd);
                    title = decodeEntities(stripTags(title)).strip();
                }
                
                // The snippet is usually in a div after the h3.
                String snippet = "";
                int divStart = html.indexOf("<div", titleEnd);
                if (divStart > 0) {
                    int snippetStart = html.indexOf(">", divStart) + 1;
                    int snippetEnd = html.indexOf("</div>", snippetStart);
                    if (snippetEnd > snippetStart && snippetEnd - snippetStart < 500) {
                        snippet = html.substring(snippetStart, snippetEnd);
                        snippet = decodeEntities(stripTags(snippet)).strip();
                    }
                }

                if (!title.isEmpty() && !url.isEmpty() && !url.startsWith("https://accounts.google.com")) {
                    results.add(new String[]{title, snippet, url});
                }
                pos = titleEnd > 0 ? titleEnd : urlEnd;
            }
            
            return results;
        }
        
        private static String stripTags(String s) {
            return s == null ? "" : s.replaceAll("<[^>]+>", "");
        }

        private static String decodeEntities(String s) {
            return s == null ? "" : s
                    .replace("&amp;",  "&").replace("&lt;",   "<").replace("&gt;",  ">")
                    .replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ")
                    .replace("&#39;",  "'").replaceAll("\\s+", " ");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared HTTP helper
    // ─────────────────────────────────────────────────────────────────────────

    private static String fetchTextWithSslFallback(String urlStr, String acceptHeader) throws Exception {
        if (Boolean.getBoolean("wayang.web_search.disable_ssl_verify") && urlStr.startsWith("https://")) {
            return fetchRaw(urlStr, acceptHeader, true);
        }
        try {
            return fetchRaw(urlStr, acceptHeader, false);
        } catch (javax.net.ssl.SSLException e) {
            return fetchRaw(urlStr, acceptHeader, true);
        }
    }

    private static String fetchRaw(String urlStr, String acceptHeader, boolean skipSsl) throws Exception {
        HttpURLConnection conn = skipSsl && urlStr.startsWith("https://")
                ? openTrustAllConnection(urlStr)
                : (HttpURLConnection) URI.create(urlStr).toURL().openConnection();

        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; Wayang-AI-Agent/1.0)");
        conn.setRequestProperty("Accept", acceptHeader);
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.connect();

        int status = conn.getResponseCode();
        if (status >= 400) throw new RuntimeException("HTTP " + status + " from " + urlStr);

        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("TrustAllX509TrustManager")
    private static HttpURLConnection openTrustAllConnection(String urlStr) throws Exception {
        TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] c, String a) {}
            public void checkServerTrusted(X509Certificate[] c, String a) {}
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, tm, new SecureRandom());
        HttpsURLConnection conn = (HttpsURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setHostnameVerifier((h, s) -> true);
        return conn;
    }
}
