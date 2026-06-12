package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeProbeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One-shot bootstrap readiness report for Agentic Commerce Wayang wiring.
 */
public record AgenticCommerceWayangBootstrapReport(
        AgenticCommerceWayangRuntimeConfig runtimeConfig,
        AgenticCommerceWayangBootstrapConfig bootstrapConfig,
        String connector,
        AgenticCommerceSkillRegistration skillRegistration,
        AgenticCommerceCheckoutHttpSmokeProbeResult smokeProbe,
        AgenticCommerceHttpBindingReport bindingReport,
        Map<String, Object> metadata) {

    public AgenticCommerceWayangBootstrapReport {
        runtimeConfig = runtimeConfig == null ? AgenticCommerceWayangRuntimeConfig.defaults() : runtimeConfig;
        bootstrapConfig = bootstrapConfig == null ? AgenticCommerceWayangBootstrapConfig.defaults() : bootstrapConfig;
        connector = AgenticCommerceWayangMaps.text(connector);
        skillRegistration = Objects.requireNonNull(skillRegistration, "skillRegistration");
        smokeProbe = Objects.requireNonNull(smokeProbe, "smokeProbe");
        bindingReport = Objects.requireNonNull(bindingReport, "bindingReport");
        metadata = AgenticCommerceWayangMaps.copy(metadata);
    }

    public static AgenticCommerceWayangBootstrapReport from(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceSkillRegistration skillRegistration) {
        return from(runtime, skillRegistration, AgenticCommerceWayangBootstrapConfig.defaults());
    }

    public static AgenticCommerceWayangBootstrapReport from(
            AgenticCommerceWayangRuntime runtime,
            AgenticCommerceSkillRegistration skillRegistration,
            AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangRuntime resolved = Objects.requireNonNull(runtime, "runtime");
        AgenticCommerceSkillRegistration registration = Objects.requireNonNull(skillRegistration, "skillRegistration");
        AgenticCommerceWayangBootstrapConfig config = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        AgenticCommerceCheckoutHttpSmokeProbeResult smokeProbe = resolved.httpConfig().smokeEnabled()
                ? resolved.smokeProbe()
                : AgenticCommerceCheckoutHttpSmokeProbeResult.fromSummary(resolved.smoke().summary());
        return new AgenticCommerceWayangBootstrapReport(
                resolved.runtimeConfig(),
                config,
                resolved.connector().getClass().getName(),
                registration,
                smokeProbe,
                resolved.bindingReport(),
                metadata(resolved));
    }

    public boolean ready() {
        return (!bootstrapConfig.requireSkillRegistration() || skillRegistration.successful())
                && (!bootstrapConfig.requireSmokeProbe() || smokeProbe.passed())
                && (!bootstrapConfig.requireBindingRoutes() || bindingReport.routeCount() > 0);
    }

    public boolean failed() {
        return !ready();
    }

    public int exitCode() {
        return ready() ? 0 : 1;
    }

    public int bootstrapIssueCount() {
        return bootstrapIssues().size();
    }

    public int issueCount() {
        return bootstrapIssueCount() + smokeProbe.issueCount();
    }

    public List<String> bootstrapIssues() {
        List<String> issues = new ArrayList<>();
        if (bootstrapConfig.requireSkillRegistration() && !skillRegistration.successful()) {
            issues.add("skill_registration_incomplete");
        }
        if (bootstrapConfig.requireSmokeProbe() && !smokeProbe.passed()) {
            issues.add("checkout_smoke_probe_failed");
        }
        if (bootstrapConfig.requireBindingRoutes() && bindingReport.routeCount() == 0) {
            issues.add("checkout_binding_routes_missing");
        }
        return List.copyOf(issues);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("ready", ready());
        values.put("failed", failed());
        values.put("exitCode", exitCode());
        values.put("connector", connector);
        values.put("issueCount", issueCount());
        values.put("bootstrapIssueCount", bootstrapIssueCount());
        values.put("bootstrapIssues", bootstrapIssues());
        values.put("runtimeConfig", runtimeConfig.toMap());
        values.put("bootstrapConfig", bootstrapConfig.toMap());
        values.put("skillRegistration", skillRegistration.toMap());
        values.put("smokeProbe", smokeProbe.toMap());
        values.put("bindingReport", bindingReport.toMap());
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }

    private static Map<String, Object> metadata(AgenticCommerceWayangRuntime runtime) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("smokeEndpointEnabled", runtime.httpConfig().smokeEnabled());
        values.put("bindingReportEndpointEnabled", runtime.httpConfig().bindingReportEnabled());
        values.put("skillCount", runtime.skillDispatcher().skillIds().size());
        values.put("operationCount", runtime.skillDispatcher().operations().size());
        return Map.copyOf(values);
    }
}
