package tech.kayys.wayang.rag.core.impl;

import tech.kayys.wayang.rag.core.RagDocument;
import tech.kayys.wayang.rag.core.spi.DocumentParser;

import java.util.Map;

public class SimpleTextDocumentParser implements DocumentParser {

    @Override
    public RagDocument parse(String source, String rawContent, Map<String, Object> metadata) {
        String normalized = rawContent == null ? "" : rawContent.trim();
        return RagDocument.of(normalized, metadata);
    }
}
