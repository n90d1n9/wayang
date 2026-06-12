package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Named target boundaries for the skill-management package split.
 */
public enum SkillManagementModuleBoundary {
    FACADE("", "facade", "Source-compatible service, factory, and admin facade entry points"),
    CONFIG("config", "config", "Configuration records, parsers, profiles, and validation helpers"),
    CONTRACTS("contracts", "contracts", "Persistence contract, capability, and deployment matrix models"),
    PREFLIGHT("preflight", "preflight", "Side-effect-free readiness and capability checks"),
    RUNTIME("runtime", "runtime", "Runtime dependency assembly and normalized component graphs"),
    STORE("store", "store", "Persistence interfaces, concrete stores, and store factories"),
    WORKFLOW("workflow", "workflow", "Bootstrap, maintenance, deployment, sync, and reconcile workflows"),
    EVENTS("events", "events", "Event sinks, readers, history, pruning, and attribute helpers"),
    ADMIN("admin", "admin", "Operator-facing DTOs and view mappers"),
    SUPPORT("support", "support", "Small shared utilities that should stay package-internal");

    private static final String BASE_PACKAGE = "tech.kayys.wayang.agent.skills.management";

    private final String packageSuffix;
    private final String label;
    private final String responsibility;

    SkillManagementModuleBoundary(String packageSuffix, String label, String responsibility) {
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

    public static List<SkillManagementModuleBoundary> targetSubpackages() {
        return List.of(values()).stream()
                .filter(SkillManagementModuleBoundary::targetSubpackage)
                .toList();
    }
}
