package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagResult;

final class RagResponseContent {

    private RagResponseContent() {
    }

    static String answer(RagResult result) {
        return result == null || result.answer() == null ? "" : result.answer();
    }

    static String context(RagResult result) {
        return answer(result);
    }
}
