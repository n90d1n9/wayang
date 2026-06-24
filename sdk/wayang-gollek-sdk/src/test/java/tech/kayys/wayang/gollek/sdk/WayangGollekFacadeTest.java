package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class WayangGollekFacadeTest {

    @Test
    public void testListModelsUsesSdkWhenAvailable() throws Exception {
        List<?> models = WayangGollekFacade.listModels();
        assertNotNull(models, "models should not be null");
        assertTrue(models.size() >= 2, "expected at least 2 models from test SDK");
        assertTrue(models.get(0).toString().contains("test-model-1"));
    }

    @Test
    public void testModelExistsViaSdk() throws Exception {
        boolean exists = WayangGollekFacade.modelExists("test-model-1");
        assertTrue(exists, "test-model-1 should be reported as existing by test SDK");

        boolean notExists = WayangGollekFacade.modelExists("no-such-model-xyz");
        assertFalse(notExists, "unknown model should not exist");
    }
}
