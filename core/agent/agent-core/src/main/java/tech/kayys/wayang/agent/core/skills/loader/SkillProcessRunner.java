package tech.kayys.wayang.agent.core.skills.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs a filesystem skill process with bounded lifetime and output capture.
 */
final class SkillProcessRunner {

    private static final Duration TERMINATION_GRACE = Duration.ofSeconds(1);
    private static final int MAX_CAPTURE_CHARS = 64 * 1024;
    private static final int MAX_FAILURE_OUTPUT_CHARS = 2 * 1024;

    private SkillProcessRunner() {
    }

    static ProcessResult run(ProcessBuilder processBuilder, int timeoutSeconds)
            throws IOException, InterruptedException, TimeoutException {
        Objects.requireNonNull(processBuilder, "processBuilder");
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Skill execution timeout must be positive");
        }

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        OutputCapture output = OutputCapture.start(process.getInputStream());

        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            terminate(process);
            output.awaitQuietly(TERMINATION_GRACE);
            throw new TimeoutException("Skill execution timeout after " + timeoutSeconds + " seconds");
        }

        OutputCapture.CapturedOutput text = output.await();
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new ProcessFailureException(new ProcessResult(text.text().trim(), exitCode,
                    text.truncated(), text.totalChars()));
        }

        return new ProcessResult(text.text().trim(), exitCode, text.truncated(), text.totalChars());
    }

    private static void terminate(Process process) throws InterruptedException {
        ProcessHandle handle = process.toHandle();
        List<ProcessHandle> descendants = handle.descendants().toList();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(TERMINATION_GRACE.toMillis(), TimeUnit.MILLISECONDS)) {
            descendants.forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor(TERMINATION_GRACE.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    record ProcessResult(String output, int exitCode, boolean outputTruncated, long outputChars) {
        Map<String, Object> metadata() {
            return Map.of(
                    SkillExecutionMetadata.KEY_EXIT_CODE, exitCode,
                    SkillExecutionMetadata.KEY_OUTPUT_CHARS, outputChars,
                    SkillExecutionMetadata.KEY_OUTPUT_TRUNCATED, outputTruncated);
        }
    }

    static final class ProcessFailureException extends RuntimeException {
        private final ProcessResult result;

        ProcessFailureException(ProcessResult result) {
            super(errorMessage(result));
            this.result = result;
        }

        ProcessResult result() {
            return result;
        }

        Map<String, Object> metadata() {
            return SkillExecutionOutcomes.failureMetadata(
                    SkillFailureType.PROCESS_EXIT,
                    result.metadata());
        }

        private static String errorMessage(ProcessResult result) {
            String output = result.output();
            if (output == null || output.isBlank()) {
                return "Skill execution failed with exit code " + result.exitCode();
            }

            String excerpt = output.length() > MAX_FAILURE_OUTPUT_CHARS
                    ? output.substring(0, MAX_FAILURE_OUTPUT_CHARS) + "\n[output excerpt truncated]"
                    : output;
            return "Skill execution failed with exit code " + result.exitCode()
                    + ". Output:\n" + excerpt;
        }
    }

    private static final class OutputCapture {
        private final StringBuilder output = new StringBuilder();
        private final AtomicReference<IOException> failure = new AtomicReference<>();
        private final Thread thread;
        private long totalChars;
        private boolean truncated;

        private OutputCapture(InputStream inputStream) {
            thread = new Thread(() -> read(inputStream), "skill-process-output");
            thread.setDaemon(true);
        }

        static OutputCapture start(InputStream inputStream) {
            OutputCapture capture = new OutputCapture(inputStream);
            capture.thread.start();
            return capture;
        }

        CapturedOutput await() throws IOException, InterruptedException {
            thread.join();
            IOException error = failure.get();
            if (error != null) {
                throw error;
            }
            return new CapturedOutput(output.toString(), truncated, totalChars);
        }

        void awaitQuietly(Duration timeout) throws InterruptedException {
            thread.join(timeout.toMillis());
        }

        private void read(InputStream inputStream) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    append(line);
                    append("\n");
                }
            } catch (IOException e) {
                failure.set(e);
            }
        }

        private void append(String chunk) {
            totalChars += chunk.length();
            int remaining = MAX_CAPTURE_CHARS - output.length();
            if (remaining <= 0) {
                truncated = true;
                return;
            }
            if (chunk.length() <= remaining) {
                output.append(chunk);
                return;
            }
            output.append(chunk, 0, remaining);
            truncated = true;
        }

        record CapturedOutput(String text, boolean truncated, long totalChars) {
        }
    }
}
