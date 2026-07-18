package tech.kayys.wayang.guardrails.node;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GuardrailNodeProvider implements NodeProvider {

        @Override
        public String id() {
                return "tech.kayys.wayang.guardrails";
        }

        @Override
        public String name() {
                return "Guardrails Plugin";
        }

        @Override
        public String version() {
                return "1.0.0";
        }

        @Override
        public String description() {
                return "Provides PII, toxicity, bias and hallucination detection nodes.";
        }

        @Override
        public List<NodeDefinition> nodes() {
                return List.of(
                                new NodeDefinition(
                                                GuardrailNodeTypes.GUARDRAIL_PII,
                                                "PII Detector",
                                                "Guardrail",
                                                "Detection",
                                                "Detects and redacts Personally Identifiable Information (SSN, Phone, Email, etc.)",
                                                "shield-search",
                                                "#EF4444",
                                                GuardrailSchemas.GUARDRAIL_CONFIG,
                                                "{}",
                                                "{}",
                                                Map.of("threshold", 0.7, "blocking", true, "redact", true)),
                                new NodeDefinition(
                                                GuardrailNodeTypes.GUARDRAIL_TOXICITY,
                                                "Toxicity Detector",
                                                "Guardrail",
                                                "Detection",
                                                "Detects toxic, hateful, or abusive content",
                                                "alert-triangle",
                                                "#F59E0B",
                                                GuardrailSchemas.GUARDRAIL_CONFIG,
                                                "{}",
                                                "{}",
                                                Map.of("threshold", 0.5, "blocking", true)),
                                new NodeDefinition(
                                                GuardrailNodeTypes.GUARDRAIL_BIAS,
                                                "Bias Detector",
                                                "Guardrail",
                                                "Detection",
                                                "Detects social bias, stereotyping, or unfair treatment",
                                                "users",
                                                "#8B5CF6",
                                                GuardrailSchemas.GUARDRAIL_CONFIG,
                                                "{}",
                                                "{}",
                                                Map.of("threshold", 0.5, "blocking", true)),
                                new NodeDefinition(
                                                GuardrailNodeTypes.GUARDRAIL_HALLUCINATION,
                                                "Hallucination Detector",
                                                "Guardrail",
                                                "Detection",
                                                "Detects factual inconsistencies or made-up information",
                                                "eye-off",
                                                "#10B981",
                                                GuardrailSchemas.GUARDRAIL_CONFIG,
                                                "{}",
                                                "{}",
                                                Map.of("threshold", 0.6, "blocking", true)),
                                new NodeDefinition(
                                                GuardrailNodeTypes.GUARDRAIL_VALIDATE,
                                                "Guardrail Validator",
                                                "Guardrail",
                                                "Orchestration",
                                                "Orchestrates multiple guardrail checks in a single node",
                                                "shield-check",
                                                "#3B82F6",
                                                GuardrailSchemas.GUARDRAIL_VALIDATE_CONFIG,
                                                "{}",
                                                "{}",
                                                Map.of("detectors", List.of("pii", "toxicity"), "failFast", false)));
        }
}
