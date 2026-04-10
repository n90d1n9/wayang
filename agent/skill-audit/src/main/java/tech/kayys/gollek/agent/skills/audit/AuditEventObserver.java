package tech.kayys.gollek.agent.skills.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Audit event observer that persists audit events to file.
 */
@ApplicationScoped
@Startup
public class AuditEventObserver {

    private static final Logger log = LoggerFactory.getLogger(AuditEventObserver.class);
    private static final Path AUDIT_DIR = Paths.get(System.getProperty("user.home"), ".gollek", "audit");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Observe and persist audit events.
     */
    void observeAuditEvent(@Observes AuditEvent event) {
        try {
            persistToFile(event);
            logToConsole(event);
        } catch (Exception e) {
            log.error("Failed to persist audit event: {}", event.eventId(), e);
        }
    }

    private void persistToFile(AuditEvent event) throws IOException {
        Files.createDirectories(AUDIT_DIR);
        
        String date = event.timestamp().toEpochMilli() / (1000 * 60 * 60 * 24);
        Path auditFile = AUDIT_DIR.resolve("audit-" + date + ".log");
        
        String json = objectMapper.writeValueAsString(event);
        Files.writeString(
            auditFile,
            json + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private void logToConsole(AuditEvent event) {
        String message = String.format(
            "AUDIT [%s] %s - %s by %s on %s - %s",
            event.status(),
            event.eventType().getCode(),
            event.action(),
            event.userId(),
            event.skillId() != null ? event.skillId() : "N/A",
            event.status() == AuditStatus.FAILURE ? 
                event.details().getOrDefault("error", "Unknown error") : "OK"
        );

        switch (event.status()) {
            case SUCCESS -> log.info(message);
            case FAILURE -> log.warn(message);
            case DENIED -> log.warn("SECURITY: {}", message);
            default -> log.debug(message);
        }
    }
}
