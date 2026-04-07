package tech.kayys.wayang.tool.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RegistrySyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrySyncScheduler.class);

    @Inject
    RegistrySyncService registrySyncService;

    @Scheduled(every = "60s", delayed = "20s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void runScheduledSync() {
        try {
            registrySyncService.syncScheduled()
                    .subscribe()
                    .with(result -> LOG.debug(
                                    "Registry scheduled sync: openapiScanned={}, mcpScanned={}, warnings={}",
                                    result.openApiSourcesScanned(),
                                    result.mcpSourcesScanned(),
                                    result.warnings().size()),
                            error -> LOG.warn("Registry scheduled sync failed: {}", error.getMessage()));
        } catch (Exception e) {
            LOG.warn("Registry scheduled sync dispatch failed: {}", e.getMessage());
        }
    }
}

