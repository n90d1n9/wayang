package tech.kayys.wayang.rag.runtime;

import java.util.Map;

record DocumentSource(
                SourceType type,
                String path,
                String content,
                Map<String, String> metadata) {
}