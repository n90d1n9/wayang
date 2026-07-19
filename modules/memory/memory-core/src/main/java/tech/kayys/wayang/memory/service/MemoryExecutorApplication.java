package tech.kayys.wayang.memory.service;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gamelan Memory Executor - Main Application
 */
public class MemoryExecutorApplication implements QuarkusApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryExecutorApplication.class);

    public static void main(String[] args) {
        LOG.info("Starting Gamelan Memory Executor...");
        Quarkus.run(MemoryExecutorApplication.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("=".repeat(60));
        LOG.info("  Gamelan Memory Executor Started");
        LOG.info("  Version: 1.0.0");
        LOG.info("  Ready to process memory-aware workflow tasks");
        LOG.info("=".repeat(60));

        Quarkus.waitForExit();
        return 0;
    }
}
