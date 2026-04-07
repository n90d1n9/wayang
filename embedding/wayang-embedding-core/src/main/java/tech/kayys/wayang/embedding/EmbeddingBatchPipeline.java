package tech.kayys.wayang.embedding;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Async embedding pipeline with bounded queue, batching, and retry.
 */
public class EmbeddingBatchPipeline implements AutoCloseable {

    private final EmbeddingService embeddingService;
    private final int batchSize;
    private final int maxRetries;
    private final BlockingQueue<Job> queue;
    private final List<Thread> workers;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public EmbeddingBatchPipeline(EmbeddingService embeddingService, EmbeddingModuleConfig config) {
        this(embeddingService, config.getBatchSize(), config.getBatchQueueCapacity(), config.getBatchMaxRetries(),
                config.getBatchWorkerThreads());
    }

    public EmbeddingBatchPipeline(
            EmbeddingService embeddingService,
            int batchSize,
            int queueCapacity,
            int maxRetries,
            int workerThreads) {
        this.embeddingService = Objects.requireNonNull(embeddingService, "embeddingService must not be null");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be > 0");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (workerThreads <= 0) {
            throw new IllegalArgumentException("workerThreads must be > 0");
        }
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.workers = new ArrayList<>(workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            Thread worker = new Thread(this::runWorker, "embedding-batch-worker-" + i);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
    }

    public CompletableFuture<EmbeddingResponse> submit(String tenantId, EmbeddingRequest request) {
        if (!running.get()) {
            throw new EmbeddingException("Embedding batch pipeline is closed");
        }
        Job job = new Job(tenantId, request, new CompletableFuture<>());
        if (!queue.offer(job)) {
            throw new EmbeddingException("Embedding batch queue is full");
        }
        return job.future();
    }

    public EmbeddingResponse submitAndWait(String tenantId, EmbeddingRequest request, Duration timeout) {
        try {
            return submit(tenantId, request).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Interrupted while waiting for embedding batch result", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new EmbeddingException("Embedding batch execution failed", cause);
        } catch (TimeoutException e) {
            throw new EmbeddingException("Timed out waiting for embedding batch result", e);
        }
    }

    @Override
    public void close() {
        running.set(false);
        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    private void runWorker() {
        while (running.get()) {
            try {
                Job job = queue.poll(200, TimeUnit.MILLISECONDS);
                if (job == null) {
                    continue;
                }
                try {
                    job.future().complete(process(job.tenantId(), job.request()));
                } catch (Throwable e) {
                    job.future().completeExceptionally(e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!running.get()) {
                    return;
                }
            }
        }
    }

    private EmbeddingResponse process(String tenantId, EmbeddingRequest request) {
        List<float[]> vectors = new ArrayList<>(request.inputs().size());
        String provider = null;
        String model = null;
        String version = null;
        int dimension = -1;

        for (int start = 0; start < request.inputs().size(); start += batchSize) {
            int end = Math.min(start + batchSize, request.inputs().size());
            EmbeddingRequest slice = new EmbeddingRequest(
                    request.inputs().subList(start, end),
                    request.model(),
                    request.provider(),
                    request.normalize());

            // Blocking call for the worker thread
            EmbeddingResponse response = retry(tenantId, slice);
            if (provider == null) {
                provider = response.provider();
                model = response.model();
                version = response.version();
                dimension = response.dimension();
            } else if (response.dimension() != dimension) {
                throw new EmbeddingException(
                        "Batch embedding dimension mismatch: expected " + dimension + " but got "
                                + response.dimension());
            }
            vectors.addAll(response.embeddings());
        }

        if (provider == null) {
            throw new EmbeddingException("No embeddings generated");
        }
        return new EmbeddingResponse(vectors, dimension, provider, model, version);
    }

    private EmbeddingResponse retry(String tenantId, EmbeddingRequest request) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return embeddingService.embedForTenant(tenantId, request).await().indefinitely();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt == maxRetries) {
                    break;
                }
                sleepBackoff(attempt);
            }
        }
        throw new EmbeddingException("Failed to embed batch after " + (maxRetries + 1) + " attempts", last);
    }

    private static void sleepBackoff(int attempt) {
        long delayMs = 25L * (1L << Math.min(attempt, 5));
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record Job(String tenantId, EmbeddingRequest request, CompletableFuture<EmbeddingResponse> future) {
    }
}
