package tech.kayys.wayang.code;

import java.util.List;
import java.util.Map;

/**
 * Extension point for the Wayang coding-agent runtime.
 *
 * <p>Open-source builds can ship with no extensions, while pro or enterprise
 * jars can use {@link java.util.ServiceLoader} to add policies, telemetry,
 * billing, multitenant routing, richer tools, or prompt constraints without
 * replacing the CLI/API contract.</p>
 */
public interface WayangCodeAgentExtension {

    /**
     * Stable extension identifier.
     */
    String extensionId();

    /**
     * Human-readable extension name.
     */
    default String name() {
        return extensionId();
    }

    /**
     * Edition this extension belongs to, for example {@code oss}, {@code pro},
     * or {@code enterprise}.
     */
    default String edition() {
        return "oss";
    }

    /**
     * Lower values run first.
     */
    default int priority() {
        return 100;
    }

    /**
     * Capability tags advertised by this extension.
     */
    default List<String> capabilityTags() {
        return List.of();
    }

    /**
     * Returns whether the extension should be active for the supplied session.
     */
    default boolean supports(WayangCodeAgentContext context) {
        return true;
    }

    /**
     * Contribute declarative session behavior.
     */
    default WayangCodeAgentContribution contribute(WayangCodeAgentContext context) {
        return WayangCodeAgentContribution.empty(extensionId());
    }

    /**
     * Publish extension diagnostics for CLI/API discovery surfaces.
     */
    default WayangCodeAgentExtensionDiagnostics diagnostics(WayangCodeAgentContext context) {
        return new WayangCodeAgentExtensionDiagnostics(
                extensionId(),
                getClass().getName(),
                name(),
                edition(),
                priority(),
                supports(context),
                capabilityTags(),
                supports(context) ? "available" : "not active for this context",
                Map.of());
    }
}
