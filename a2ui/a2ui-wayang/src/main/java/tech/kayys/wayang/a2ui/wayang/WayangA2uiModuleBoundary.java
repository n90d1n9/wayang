package tech.kayys.wayang.a2ui.wayang;

import java.util.List;

/**
 * Named target boundaries for the Wayang A2UI package split.
 */
public enum WayangA2uiModuleBoundary {
    FACADE("", "facade", "Source-compatible Wayang A2UI public entry points and model contracts"),
    ACTION("action", "action", "Action policies, handlers, routing, admission, and feedback assembly"),
    BRIDGE("bridge", "bridge", "Transport-neutral bridge requests, responses, scenarios, and harnesses"),
    HTTP("http", "http", "HTTP adapters, routes, probes, diagnostics, scenarios, and smoke harnesses"),
    PROJECTION("projection", "projection", "Ordered JSON/map projection helpers for contract-stable payloads"),
    SESSION("session", "session", "Session decoding, configuration, state, profiles, and session results"),
    SPEC("spec", "spec", "A2UI specification-alignment reports, requirements, and standard descriptors"),
    SURFACE("surface", "surface", "Surface catalog, registry, renderers, result views, and run surfaces"),
    TRANSPORT("transport", "transport", "Transport envelopes, metadata, content, errors, outcomes, and codecs"),
    SUPPORT("support", "support", "Small package-local normalization and collection utilities");

    private static final String BASE_PACKAGE = "tech.kayys.wayang.a2ui.wayang";

    private final String packageSuffix;
    private final String label;
    private final String responsibility;

    WayangA2uiModuleBoundary(String packageSuffix, String label, String responsibility) {
        this.packageSuffix = packageSuffix;
        this.label = label;
        this.responsibility = responsibility;
    }

    public String packageName() {
        return packageSuffix.isBlank() ? BASE_PACKAGE : BASE_PACKAGE + "." + packageSuffix;
    }

    public String packageSuffix() {
        return packageSuffix;
    }

    public String label() {
        return label;
    }

    public String responsibility() {
        return responsibility;
    }

    public boolean rootPackage() {
        return packageSuffix.isBlank();
    }

    public boolean targetSubpackage() {
        return !rootPackage();
    }

    public static String basePackage() {
        return BASE_PACKAGE;
    }

    public static List<WayangA2uiModuleBoundary> targetSubpackages() {
        return List.of(values()).stream()
                .filter(WayangA2uiModuleBoundary::targetSubpackage)
                .toList();
    }
}
