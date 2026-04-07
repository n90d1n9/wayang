package tech.kayys.wayang.rag.core.spi;

import tech.kayys.wayang.rag.core.RagDocument;

import java.util.Map;

public interface DocumentParser {
    RagDocument parse(String source, String rawContent, Map<String, Object> metadata);
}
