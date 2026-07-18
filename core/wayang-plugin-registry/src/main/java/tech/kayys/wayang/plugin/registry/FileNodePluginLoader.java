package tech.kayys.wayang.plugin.registry;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;
import tech.kayys.wayang.schema.catalog.BuiltinSchemaCatalog;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ServiceLoader;

/**
 * Dynamically loads NodeProvider plugins from the user's ~/.wayang/plugins directory
 * at application startup.
 */
@Startup
@ApplicationScoped
public class FileNodePluginLoader {

    private static final Logger LOG = LoggerFactory.getLogger(FileNodePluginLoader.class);
    
    @Inject
    ControlPlaneNodeRegistry nodeRegistry;

    @PostConstruct
    public void loadExternalPlugins() {
        String homeDir = System.getProperty("user.home");
        File pluginsDir = new File(homeDir, ".wayang/plugins");
        
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            LOG.info("External plugin directory {} does not exist. Skipping external plugin loading.", pluginsDir.getAbsolutePath());
            return;
        }
        
        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            LOG.info("No external plugin JARs found in {}", pluginsDir.getAbsolutePath());
            return;
        }

        for (File jarFile : jarFiles) {
            try {
                LOG.info("Loading external Wayang plugin from: {}", jarFile.getAbsolutePath());
                URL jarUrl = jarFile.toURI().toURL();
                
                // Use a separate classloader for each plugin JAR
                PluginClassLoader pluginClassLoader = new PluginClassLoader(new URL[]{jarUrl}, getClass().getClassLoader());
                
                // Discover NodeProvider implementations inside this JAR
                ServiceLoader<NodeProvider> serviceLoader = ServiceLoader.load(NodeProvider.class, pluginClassLoader);
                
                for (NodeProvider provider : serviceLoader) {
                    LOG.info("Discovered NodeProvider: {}", provider.getClass().getName());
                    
                    // Register schemas into the global BuiltinSchemaCatalog
                    BuiltinSchemaCatalog.register(provider);
                    
                    // Register node metadata into the Control Plane registry
                    for (NodeDefinition def : provider.nodes()) {
                        // We map the SPI NodeDefinition to the internal registry NodeDefinition
                        // Note: Assuming NodeDefinition is also relocated or imported correctly
                        tech.kayys.wayang.plugin.registry.node.NodeDefinition internalDef = new tech.kayys.wayang.plugin.registry.node.NodeDefinition();
                        internalDef.type = def.type();
                        internalDef.label = def.label();
                        internalDef.category = def.category();
                        internalDef.subCategory = def.subCategory();
                        internalDef.description = def.description();
                        // Note: To fully support the internal registry, we'd need to convert the schemas to JsonSchema objects. 
                        // For now this demonstrates the discovery and link.
                        nodeRegistry.register(internalDef);
                        LOG.info("Registered external Node: {} ({})", def.type(), def.label());
                    }
                }
                
            } catch (MalformedURLException e) {
                LOG.error("Invalid URL for plugin JAR {}", jarFile.getName(), e);
            } catch (Exception e) {
                LOG.error("Failed to load external plugin from {}", jarFile.getName(), e);
            }
        }
    }
}
