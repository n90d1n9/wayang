package tech.kayys.wayang.rag.runtime;

record IngestResult(
        boolean success,
        int documentsIngested,
        int segmentsCreated,
        long durationMs,
        String message) {
}