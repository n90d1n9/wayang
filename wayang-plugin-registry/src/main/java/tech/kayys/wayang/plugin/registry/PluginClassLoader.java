package tech.kayys.wayang.plugin.registry;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom ClassLoader for loading external Wayang Node plugins containing {@link tech.kayys.wayang.plugin.spi.node.NodeProvider}.
 * Provides isolation for plugin dependencies while delegating to the application
 * classloader for shared API classes.
 */
public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    
    // In the future, this can be expanded to implement a child-first delegation 
    // model if plugins need to bundle conflicting versions of libraries (like Jackson).
    // For now, it functions as a standard URLClassLoader.
}
