package tech.kayys.wayang.guardrails.policy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import dev.cel.runtime.CelEvaluationException;
import tech.kayys.wayang.guardrails.plugin.api.*;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PolicyEngine {

    @Inject
    PolicyRepository policyRepository;

    @Inject
    Instance<GuardrailPolicyPlugin> policyPlugins;

    private final CelCompiler celCompiler;
    private final CelRuntime celRuntime;

    public PolicyEngine() {
        this.celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
                .addVar("input", SimpleType.DYN)
                .addVar("context", SimpleType.DYN)
                .addVar("tenant", SimpleType.STRING)
                .addVar("user", SimpleType.STRING)
                .build();

        this.celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
    }

    public Uni<PolicyEvaluationResult> evaluatePolicies(
            NodeContext context,
            CheckPhase phase) {
        return Uni.combine().all()
                .unis(
                        evaluateTraditionalPolicies(context, phase),
                        evaluatePluginPolicies(context, phase))
                .combinedWith(results -> {
                    List<PolicyCheckResult> traditionalResults = (List<PolicyCheckResult>) results.get(0);
                    List<PolicyCheckResult> pluginResults = (List<PolicyCheckResult>) results.get(1);

                    List<PolicyCheckResult> allResults = new ArrayList<>();
                    allResults.addAll(traditionalResults);
                    allResults.addAll(pluginResults);

                    return aggregateResults(allResults);
                });
    }

    private Uni<List<PolicyCheckResult>> evaluateTraditionalPolicies(
            NodeContext context,
            CheckPhase phase) {
        return policyRepository.findActivePolices(context.tenantId(), phase)
                .flatMap(policies -> {
                    if (policies.isEmpty()) {
                        return Uni.createFrom().item(new ArrayList<PolicyCheckResult>());
                    }
                    List<Uni<PolicyCheckResult>> evaluations = policies.stream()
                            .map(policy -> evaluatePolicy(policy, context))
                            .toList();

                    return Uni.join().all(evaluations).andFailFast();
                });
    }

    private Uni<List<PolicyCheckResult>> evaluatePluginPolicies(
            NodeContext context,
            CheckPhase phase) {

        List<GuardrailPolicyPlugin> applicablePlugins = getPolicyPluginsForPhase(phase);

        if (applicablePlugins.isEmpty()) {
            return Uni.createFrom().item(new ArrayList<>());
        }

        List<Uni<PolicyCheckResult>> pluginEvaluations = applicablePlugins.stream()
                .map(plugin -> evaluatePolicy(plugin, context, phase))
                .collect(Collectors.toList());

        return Uni.join().all(pluginEvaluations).andFailFast();
    }

    private List<GuardrailPolicyPlugin> getPolicyPluginsForPhase(CheckPhase phase) {
        List<GuardrailPolicyPlugin> plugins = new ArrayList<>();

        if (policyPlugins.isResolvable()) {
            for (GuardrailPolicyPlugin plugin : policyPlugins) {
                if (Arrays.asList(plugin.applicablePhases()).contains(phase)) {
                    plugins.add(plugin);
                }
            }
        }

        return plugins;
    }

    private Uni<PolicyCheckResult> evaluatePolicy(GuardrailPolicyPlugin plugin, NodeContext context, CheckPhase phase) {
        return plugin.evaluate(context)
                .map(result -> {
                    boolean allowed = result.allowed();
                    String denyMessage = allowed ? null : result.denyMessage();
                    return new PolicyCheckResult(
                            plugin.id(),
                            plugin.id(), // Using ID as name if not available
                            allowed,
                            denyMessage);
                });
    }

    private Uni<PolicyCheckResult> evaluatePolicy(Policy policy, NodeContext context) {
        try {
            CelAbstractSyntaxTree ast = celCompiler.compile(policy.expression()).getAst();

            Map<String, Object> celContext = Map.of(
                    "input", context.inputs(),
                    "context", context,
                    "tenant", context.tenantId(),
                    "user", context.metadata().userId());

            Object result = celRuntime.createProgram(ast).eval(celContext);

            boolean allowed = Boolean.TRUE.equals(result);

            return Uni.createFrom().item(new PolicyCheckResult(
                    policy.id(),
                    policy.name(),
                    allowed,
                    allowed ? null : policy.denyMessage()));

        } catch (CelValidationException | CelEvaluationException e) {
            return Uni.createFrom().failure(
                    new PolicyEvaluationException("CEL error in policy: " + policy.id(), e));
        }
    }

    private PolicyEvaluationResult aggregateResults(List<PolicyCheckResult> results) {
        List<PolicyCheckResult> violations = results.stream()
                .filter(r -> !r.allowed())
                .toList();

        if (violations.isEmpty()) {
            return PolicyEvaluationResult.success();
        }

        return PolicyEvaluationResult.denied(
                violations.get(0).denyMessage(),
                violations.get(0).policyId());
    }
}