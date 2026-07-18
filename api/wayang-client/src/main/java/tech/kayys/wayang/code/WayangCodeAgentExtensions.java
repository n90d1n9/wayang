package tech.kayys.wayang.code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import tech.kayys.wayang.client.SdkText;

/**
 * ServiceLoader-based discovery for Wayang coding-agent extensions.
 */
public final class WayangCodeAgentExtensions {

    private WayangCodeAgentExtensions() {
    }

    public static WayangCodeAgentExtensionDiscovery discover(WayangCodeAgentContext context) {
        return discover(context, Thread.currentThread().getContextClassLoader());
    }

    public static WayangCodeAgentExtensionDiscovery discover(
            WayangCodeAgentContext context,
            ClassLoader classLoader) {
        return discover(context, load(classLoader));
    }

    public static WayangCodeAgentExtensionDiscovery discover(
            WayangCodeAgentContext context,
            List<? extends WayangCodeAgentExtension> extensions) {
        WayangCodeAgentContext resolvedContext = context == null
                ? WayangCodeAgentContext.builder().build()
                : context;
        List<WayangCodeAgentExtensionDiagnostics> diagnostics = new ArrayList<>();
        List<WayangCodeAgentContribution> contributions = new ArrayList<>();
        if (extensions != null) {
            extensions.stream()
                    .filter(extension -> extension != null)
                    .sorted(Comparator
                            .comparingInt(WayangCodeAgentExtensions::priority)
                            .thenComparing(WayangCodeAgentExtensions::extensionId))
                    .forEach(extension -> appendExtension(resolvedContext, diagnostics, contributions, extension));
        }
        return new WayangCodeAgentExtensionDiscovery(diagnostics, contributions);
    }

    public static List<WayangCodeAgentExtension> load() {
        return load(Thread.currentThread().getContextClassLoader());
    }

    static List<WayangCodeAgentExtension> load(ClassLoader classLoader) {
        ServiceLoader<WayangCodeAgentExtension> loader =
                ServiceLoader.load(WayangCodeAgentExtension.class, resolvedClassLoader(classLoader));
        List<WayangCodeAgentExtension> extensions = new ArrayList<>();
        Iterator<ServiceLoader.Provider<WayangCodeAgentExtension>> iterator = loader.stream().iterator();
        while (true) {
            ServiceLoader.Provider<WayangCodeAgentExtension> provider;
            try {
                if (!iterator.hasNext()) {
                    return List.copyOf(extensions);
                }
                provider = iterator.next();
            } catch (ServiceConfigurationError error) {
                continue;
            }
            try {
                extensions.add(provider.get());
            } catch (RuntimeException | ServiceConfigurationError error) {
                // Discovery diagnostics require an instance; failed providers are ignored here.
            }
        }
    }

    private static void appendExtension(
            WayangCodeAgentContext context,
            List<WayangCodeAgentExtensionDiagnostics> diagnostics,
            List<WayangCodeAgentContribution> contributions,
            WayangCodeAgentExtension extension) {
        String extensionId = extensionId(extension);
        String extensionClass = extension.getClass().getName();
        int priority = priority(extension);
        try {
            WayangCodeAgentExtensionDiagnostics diagnostic = extension.diagnostics(context);
            boolean supported = extension.supports(context);
            diagnostics.add(normalizeDiagnostic(diagnostic, extensionId, extensionClass, priority, supported));
            if (supported) {
                WayangCodeAgentContribution contribution = extension.contribute(context);
                if (contribution != null && !contribution.empty()) {
                    contributions.add(contribution);
                }
            }
        } catch (RuntimeException | ServiceConfigurationError error) {
            diagnostics.add(WayangCodeAgentExtensionDiagnostics.failure(
                    extensionId,
                    extensionClass,
                    priority,
                    error));
        }
    }

    private static WayangCodeAgentExtensionDiagnostics normalizeDiagnostic(
            WayangCodeAgentExtensionDiagnostics diagnostic,
            String extensionId,
            String extensionClass,
            int priority,
            boolean supported) {
        if (diagnostic == null) {
            return new WayangCodeAgentExtensionDiagnostics(
                    extensionId,
                    extensionClass,
                    extensionId,
                    "unknown",
                    priority,
                    supported,
                    List.of(),
                    supported ? "available" : "not active for this context",
                    java.util.Map.of());
        }
        return new WayangCodeAgentExtensionDiagnostics(
                SdkText.trimToDefault(diagnostic.extensionId(), extensionId),
                SdkText.trimToDefault(diagnostic.extensionClass(), extensionClass),
                SdkText.trimToDefault(diagnostic.name(), extensionId),
                diagnostic.edition(),
                diagnostic.priority(),
                supported && diagnostic.available(),
                diagnostic.capabilityTags(),
                diagnostic.message(),
                diagnostic.details());
    }

    private static int priority(WayangCodeAgentExtension extension) {
        try {
            return extension.priority();
        } catch (RuntimeException exception) {
            return 100;
        }
    }

    private static String extensionId(WayangCodeAgentExtension extension) {
        try {
            return SdkText.trimToDefault(extension.extensionId(), extension.getClass().getName());
        } catch (RuntimeException exception) {
            return extension.getClass().getName();
        }
    }

    private static ClassLoader resolvedClassLoader(ClassLoader classLoader) {
        return classLoader == null
                ? WayangCodeAgentExtension.class.getClassLoader()
                : classLoader;
    }
}
