package tech.kayys.wayang.guardrails;

public interface GuardrailsEngine {
    GuardrailResult preCheck(ExecuteNodeTask task);
    GuardrailResult postCheck(ExecuteNodeTask task, ExecutionResult result);
    void registerPolicy(GuardrailPolicy policy);
    void updatePolicy(String policyId, 
        GuardrailPolicy policy);
}
