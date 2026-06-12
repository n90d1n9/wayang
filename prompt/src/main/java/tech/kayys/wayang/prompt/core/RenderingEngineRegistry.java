package tech.kayys.wayang.prompt.core;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.EnumMap;
import java.util.Map;

/**
 * ============================================================================
 * RenderingEngineRegistry — manages different rendering engine implementations.
 * ============================================================================
 *
 * This registry manages the available rendering engines and provides access
 * to the appropriate engine based on the requested rendering strategy.
 */
@ApplicationScoped
public class RenderingEngineRegistry {
    
    private final Map<PromptVersion.RenderingStrategy, RenderingEngine> engines;
    
    @Inject
    Instance<RenderingEngine> renderingEngines;
    
    private boolean initialized = false;
    
    public RenderingEngineRegistry() {
        this.engines = new EnumMap<>(PromptVersion.RenderingStrategy.class);
    }
    
    @PostConstruct
    void initialize() {
        if (!initialized) {
            // Add built-in engines so non-CDI tests and standalone runtimes can
            // use every strategy when the module dependencies are present.
            engines.put(PromptVersion.RenderingStrategy.SIMPLE, new SimpleRenderingEngine());
            engines.put(PromptVersion.RenderingStrategy.JINJA2, new Jinja2RenderingEngine());
            engines.put(PromptVersion.RenderingStrategy.FREEMARKER, new FreeMarkerRenderingEngine());
            
            // CDI-provided engines override defaults, allowing custom variants.
            if (renderingEngines != null) {
                for (RenderingEngine engine : renderingEngines) {
                    engines.put(engine.getStrategy(), engine);
                }
            }
            
            initialized = true;
        }
    }
    
    /**
     * Gets the rendering engine for the specified strategy.
     *
     * @param strategy The rendering strategy
     * @return The rendering engine
     * @throws IllegalArgumentException if no engine is available for the strategy
     */
    public RenderingEngine forStrategy(PromptVersion.RenderingStrategy strategy) {
        if (!engines.containsKey(strategy)) {
            throw new IllegalArgumentException("No rendering engine available for strategy: " + strategy);
        }
        return engines.get(strategy);
    }
}
