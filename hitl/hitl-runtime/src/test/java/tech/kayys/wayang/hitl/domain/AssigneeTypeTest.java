package tech.kayys.wayang.hitl.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AssigneeTypeTest {

    @Test
    void shouldHaveCorrectValues() {
        assertEquals("USER", AssigneeType.USER.name());
        assertEquals("GROUP", AssigneeType.GROUP.name());
        assertEquals("ROLE", AssigneeType.ROLE.name());
    }
}