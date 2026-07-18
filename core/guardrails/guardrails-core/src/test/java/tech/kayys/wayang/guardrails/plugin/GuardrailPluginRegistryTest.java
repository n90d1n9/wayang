package tech.kayys.wayang.guardrails.plugin;

import tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

@QuarkusTest
public class GuardrailPluginRegistryTest {

    @Inject
    GuardrailPluginRegistry pluginRegistry;

    @Test
    public void testPluginDiscovery() {
        List<GuardrailDetectorPlugin> allDetectors = pluginRegistry.getAllDetectorPlugins();
        Assertions.assertFalse(allDetectors.isEmpty(), "Should have at least one detector plugin registered");

        List<GuardrailDetectorPlugin> preExecutionDetectors = pluginRegistry.getPreExecutionDetectors();
        Assertions.assertNotNull(preExecutionDetectors, "Pre-execution detectors should not be null");

        List<GuardrailDetectorPlugin> postExecutionDetectors = pluginRegistry.getPostExecutionDetectors();
        Assertions.assertNotNull(postExecutionDetectors, "Post-execution detectors should not be null");
    }

    @Test
    public void testGetDetectorsByCategory() {
        List<GuardrailDetectorPlugin> piiDetectors = pluginRegistry.getDetectorsByCategory("pii");
        Assertions.assertNotNull(piiDetectors, "PII detectors list should not be null");

        List<GuardrailDetectorPlugin> toxicityDetectors = pluginRegistry.getDetectorsByCategory("toxicity");
        Assertions.assertNotNull(toxicityDetectors, "Toxicity detectors list should not be null");
    }
}