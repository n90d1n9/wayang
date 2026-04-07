package tech.kayys.wayang.guardrails.plugin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.guardrails.plugin.api.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for guardrails plugins that manages discovery and retrieval of
 * guardrail detectors.
 */
@ApplicationScoped
public class GuardrailPluginRegistry {

    private static final Logger LOG = Logger.getLogger(GuardrailPluginRegistry.class);

    @Inject
    Instance<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> detectorPlugins;

    private volatile List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> cachedPreExecutionDetectors;
    private volatile List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> cachedPostExecutionDetectors;

    /**
     * Get all registered guardrail detector plugins.
     */
    public List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> getAllDetectorPlugins() {
        List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> plugins = new ArrayList<>();

        if (detectorPlugins.isResolvable()) {
            for (tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin plugin : detectorPlugins) {
                plugins.add(plugin);
            }
        }

        return plugins;
    }

    /**
     * Get all guardrail detector plugins applicable for pre-execution phase.
     */
    public List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> getPreExecutionDetectors() {
        if (cachedPreExecutionDetectors == null) {
            synchronized (this) {
                if (cachedPreExecutionDetectors == null) {
                    cachedPreExecutionDetectors = getAllDetectorPlugins().stream()
                            .filter(plugin -> Arrays.asList(plugin.applicablePhases())
                                    .contains(CheckPhase.PRE_EXECUTION))
                            .collect(Collectors.toList());

                    LOG.infof("Discovered %d pre-execution guardrail detectors", cachedPreExecutionDetectors.size());
                }
            }
        }

        return new ArrayList<>(cachedPreExecutionDetectors);
    }

    /**
     * Get all guardrail detector plugins applicable for post-execution phase.
     */
    public List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> getPostExecutionDetectors() {
        if (cachedPostExecutionDetectors == null) {
            synchronized (this) {
                if (cachedPostExecutionDetectors == null) {
                    cachedPostExecutionDetectors = getAllDetectorPlugins().stream()
                            .filter(plugin -> Arrays.asList(plugin.applicablePhases())
                                    .contains(CheckPhase.POST_EXECUTION))
                            .collect(Collectors.toList());

                    LOG.infof("Discovered %d post-execution guardrail detectors", cachedPostExecutionDetectors.size());
                }
            }
        }

        return new ArrayList<>(cachedPostExecutionDetectors);
    }

    /**
     * Get a specific guardrail detector by its ID.
     */
    public Optional<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> getDetectorById(String id) {
        return getAllDetectorPlugins().stream()
                .filter(plugin -> plugin.id().equals(id))
                .findFirst();
    }

    /**
     * Get all guardrail detectors by category.
     */
    public List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> getDetectorsByCategory(
            String category) {
        return getAllDetectorPlugins().stream()
                .filter(plugin -> plugin.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * Run all applicable detectors on the provided text for the given phase.
     */
    public Uni<List<DetectionResult>> runDetectorsForPhase(
            String text,
            CheckPhase phase) {
        return runDetectorsForPhase(text, phase, Map.of());
    }

    /**
     * Run all applicable detectors with metadata.
     */
    public Uni<List<DetectionResult>> runDetectorsForPhase(
            String text,
            CheckPhase phase,
            Map<String, Object> metadata) {

        List<tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin> detectors = switch (phase) {
            case PRE_EXECUTION -> getPreExecutionDetectors();
            case POST_EXECUTION -> getPostExecutionDetectors();
        };

        if (detectors.isEmpty()) {
            return Uni.createFrom().item(new ArrayList<>());
        }

        List<Uni<DetectionResult>> detectorUnis = detectors.stream()
                .map(detector -> detector.detect(text, metadata))
                .collect(Collectors.toList());

        return Uni.combine().all().unis(detectorUnis)
                .with(results -> results.stream()
                        .map(r -> (DetectionResult) r)
                        .collect(Collectors.toList()));
    }
}