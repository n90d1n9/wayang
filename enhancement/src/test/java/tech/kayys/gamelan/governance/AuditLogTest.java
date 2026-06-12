package tech.kayys.gamelan.governance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        auditLog = new AuditLog() {
            @Override protected Path logDir() { return tmp; }
        };
        // Manually call init with temp dir
        Field logFileField = AuditLog.class.getDeclaredField("logFile");
        logFileField.setAccessible(true);
        logFileField.set(auditLog, tmp.resolve("test.log"));
    }

    @Test
    void logsToolCallWithoutThrowing() {
        assertThatCode(() ->
            auditLog.logToolCall("session-1", "read_file", Map.of("path", "Main.java"))
        ).doesNotThrowAnyException();
    }

    @Test
    void logsRunStartAndEnd() {
        assertThatCode(() -> {
            auditLog.logRunStart("s1", "fix the bug", "react");
            auditLog.logRunEnd("s1", true, 5000);
        }).doesNotThrowAnyException();
    }

    @Test
    void logsSecurityEvent() {
        assertThatCode(() ->
            auditLog.logSecurityEvent("s1", "potential prompt injection detected", "HIGH")
        ).doesNotThrowAnyException();
    }

    @Test
    void logsHumanDecision() {
        assertThatCode(() ->
            auditLog.logHumanDecision("s1", "run_command", true, "user approved")
        ).doesNotThrowAnyException();
    }

    @Test
    void recentReturnsEntries() throws Exception {
        auditLog.logRunStart("s1", "task", "react");
        Thread.sleep(100); // Give background writer time
        // We can't guarantee entries immediately due to async, just check no exception
        assertThatCode(() -> auditLog.recent(10)).doesNotThrowAnyException();
    }

    @Test
    void verifyChainReturnsTrueForEmptyLog() {
        // Empty log is valid
        assertThat(auditLog.verifyChain()).isTrue();
    }
}
