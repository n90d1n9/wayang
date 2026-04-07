package tech.kayys.wayang.guardrails.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.error.ErrorInfo;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.sdk.executor.core.AbstractWorkflowExecutor;
import tech.kayys.gamelan.sdk.executor.core.Executor;
import tech.kayys.gamelan.sdk.executor.core.SimpleNodeExecutionResult;
import tech.kayys.wayang.guardrails.GuardrailsService;
import tech.kayys.wayang.guardrails.plugin.api.*;

import tech.kayys.wayang.guardrails.node.GuardrailNodeTypes;
import tech.kayys.wayang.guardrails.plugin.GuardrailPluginRegistry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Executor for Guardrail nodes, supporting PII, Toxicity, Bias, and
 * Hallucination detection.
 */
@ApplicationScoped
@Executor(executorType = "guardrail-executor", supportedNodeTypes = {
                GuardrailNodeTypes.GUARDRAIL_PII,
                GuardrailNodeTypes.GUARDRAIL_TOXICITY,
                GuardrailNodeTypes.GUARDRAIL_BIAS,
                GuardrailNodeTypes.GUARDRAIL_HALLUCINATION,
                GuardrailNodeTypes.GUARDRAIL_VALIDATE
}, description = "Executes guardrail checks for input/output validation")
public class GuardrailNodeExecutor extends AbstractWorkflowExecutor {

        private static final Logger LOG = LoggerFactory.getLogger(GuardrailNodeExecutor.class);

        @Inject
        GuardrailPluginRegistry pluginRegistry;

        @Inject
        GuardrailsService guardrailsService;

        @Override
        public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
                String nodeType = extractNodeType(task);
                Map<String, Object> config = task.context();

                // Extract text input - assuming "text" is the primary input key
                Object inputText = config.get("text");
                if (inputText == null) {
                        LOG.warn("Required input 'text' missing for guardrail node: {}", task.nodeId().value());
                        return Uni.createFrom().item(SimpleNodeExecutionResult.failure(
                                        task.runId(), task.nodeId(), task.attempt(),
                                        ErrorInfo.of(new IllegalArgumentException("Missing required input 'text'")),
                                        task.token()));
                }

                String text = inputText.toString();

                if (GuardrailNodeTypes.GUARDRAIL_VALIDATE.equals(nodeType)) {
                        return guardrailsService.preCheck(text, config)
                                        .map(result -> {
                                                Map<String, Object> output = Map.of(
                                                                "allowed", result.allowed(),
                                                                "reason",
                                                                result.reason() != null ? result.reason() : "",
                                                                "findings", result.findings());
                                                return SimpleNodeExecutionResult.success(
                                                                task.runId(), task.nodeId(), task.attempt(), output,
                                                                task.token(),
                                                                java.time.Duration.ZERO);
                                        })
                                        .onFailure().recoverWithItem(throwable -> {
                                                LOG.error("Guardrail validation failed: {}", throwable.getMessage());
                                                return SimpleNodeExecutionResult.failure(
                                                                task.runId(), task.nodeId(), task.attempt(),
                                                                ErrorInfo.of(throwable), task.token());
                                        });
                }

                // Direct detector execution for simple nodes
                String category = getCategoryForNodeType(nodeType);
                return pluginRegistry
                                .runDetectorsForPhase(text, CheckPhase.PRE_EXECUTION)
                                .map(results -> {
                                        List<DetectionResult> filtered = results.stream()
                                                        .filter(r -> r.detectorId().toLowerCase().contains(category))
                                                        .collect(Collectors.toList());

                                        DetectionResults detectionResults = new DetectionResults(filtered);
                                        boolean allowed = !detectionResults.hasBlockingIssues();

                                        Map<String, Object> output = Map.of(
                                                        "allowed", allowed,
                                                        "findings", filtered);

                                        return SimpleNodeExecutionResult.success(
                                                        task.runId(), task.nodeId(), task.attempt(), output,
                                                        task.token(), java.time.Duration.ZERO);
                                })
                                .onFailure().recoverWithItem(throwable -> {
                                        LOG.error("Detector execution failed: {}", throwable.getMessage());
                                        return SimpleNodeExecutionResult.failure(
                                                        task.runId(), task.nodeId(), task.attempt(),
                                                        ErrorInfo.of(throwable), task.token());
                                });
        }

        private String getCategoryForNodeType(String nodeType) {
                return switch (nodeType) {
                        case GuardrailNodeTypes.GUARDRAIL_PII -> "pii";
                        case GuardrailNodeTypes.GUARDRAIL_TOXICITY -> "toxicity";
                        case GuardrailNodeTypes.GUARDRAIL_BIAS -> "bias";
                        case GuardrailNodeTypes.GUARDRAIL_HALLUCINATION -> "hallucination";
                        default -> "validation";
                };
        }
}
