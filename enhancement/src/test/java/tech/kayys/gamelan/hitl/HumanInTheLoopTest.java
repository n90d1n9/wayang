package tech.kayys.gamelan.hitl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.governance.AuditLog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HumanInTheLoopTest {

    @Mock AuditLog          auditLog;
    @InjectMocks HumanInTheLoop hitl;

    @BeforeEach
    void setUp() { hitl.setAutoApprove(true); }

    @Test void autoApproveBypassesGate() {
        assertThat(hitl.requestApproval("s1", "run_command", "ls")).isTrue();
        verify(auditLog).logHumanDecision(eq("s1"), eq("run_command"), eq(true), anyString());
    }

    @Test void disabledToolNeedsNoApproval() {
        hitl.setAutoApprove(false);
        assertThat(hitl.requestApproval("s1", "read_file", "Main.java")).isTrue();
        verifyNoInteractions(auditLog);
    }

    @Test void writeOpsHaveGates() {
        assertThat(hitl.hasGate("write_file")).isTrue();
        assertThat(hitl.hasGate("apply_patch")).isTrue();
        assertThat(hitl.hasGate("run_command")).isTrue();
        assertThat(hitl.hasGate("http_post")).isTrue();
        assertThat(hitl.hasGate("http_delete")).isTrue();
    }

    @Test void readOpsHaveNoGate() {
        assertThat(hitl.hasGate("read_file")).isFalse();
        assertThat(hitl.hasGate("search_files")).isFalse();
        assertThat(hitl.hasGate("glob")).isFalse();
    }

    @Test void canRegisterCustomGate() {
        hitl.register("custom_tool", HumanInTheLoop.GateType.ALWAYS);
        assertThat(hitl.hasGate("custom_tool")).isTrue();
    }

    @Test void registeredGatesIsUnmodifiable() {
        assertThatThrownBy(() -> hitl.registeredGates().put("x", HumanInTheLoop.GateType.ALWAYS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void autoApproveToggle() {
        hitl.setAutoApprove(true);  assertThat(hitl.isAutoApprove()).isTrue();
        hitl.setAutoApprove(false); assertThat(hitl.isAutoApprove()).isFalse();
    }
}
