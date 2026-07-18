package tech.kayys.wayang.guardrails.node;

public final class GuardrailNodeTypes {

    private GuardrailNodeTypes() {
    }

    public static final String GUARDRAIL_PII = "guardrail-pii";
    public static final String GUARDRAIL_TOXICITY = "guardrail-toxicity";
    public static final String GUARDRAIL_BIAS = "guardrail-bias";
    public static final String GUARDRAIL_HALLUCINATION = "guardrail-hallucination";
    public static final String GUARDRAIL_VALIDATE = "guardrail-validate";
}
