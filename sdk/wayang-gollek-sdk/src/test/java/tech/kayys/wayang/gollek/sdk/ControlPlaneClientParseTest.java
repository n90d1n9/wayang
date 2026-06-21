package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ControlPlaneClientParseTest {

    @Test
    public void testAllocateResponseDeserialization() throws Exception {
        String json = "{\"id\":\"abc-123\",\"path\":\"/dev/shm/yaff-abc.dat\",\"offset\":0,\"length\":42}";
        ObjectMapper m = new ObjectMapper();
        ControlPlaneClient.AllocateResponse ar = m.readValue(json, ControlPlaneClient.AllocateResponse.class);
        assertEquals("abc-123", ar.id);
        assertEquals("/dev/shm/yaff-abc.dat", ar.path);
        assertEquals(0L, ar.offset);
        assertEquals(42L, ar.length);
    }
}
