package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Versioned contract describing the Hermes metadata envelope emitted at runtime.
 */
public record HermesMetadataContract(
        String id,
        int schemaVersion,
        String mode,
        List<String> contextPlanKeys,
        List<String> parameterPlanKeys,
        List<String> contextDirectiveKeys,
        List<String> parameterDirectiveKeys,
        List<String> contextRuntimeKeys,
        List<String> parameterRuntimeKeys,
        List<String> directiveDispatchReportFields,
        List<String> directiveDispatchSummaryFields,
        List<String> directiveDispatchAttentionFields,
        List<String> directiveDispatchRemediationFields,
        List<String> directiveDispatchRemediationActionFields,
        List<String> directiveDispatchResultFields,
        List<String> runtimePortDescriptorFields,
        List<String> runtimeEventFields) {

    public static final String CURRENT_ID = "wayang.hermes.metadata";
    public static final int CURRENT_SCHEMA_VERSION = 9;

    public HermesMetadataContract {
        id = id == null || id.isBlank() ? CURRENT_ID : id.trim();
        mode = mode == null || mode.isBlank() ? HermesAgentMode.MODE_ID : mode.trim();
        contextPlanKeys = contextPlanKeys == null ? List.of() : List.copyOf(contextPlanKeys);
        parameterPlanKeys = parameterPlanKeys == null ? List.of() : List.copyOf(parameterPlanKeys);
        contextDirectiveKeys = contextDirectiveKeys == null ? List.of() : List.copyOf(contextDirectiveKeys);
        parameterDirectiveKeys = parameterDirectiveKeys == null ? List.of() : List.copyOf(parameterDirectiveKeys);
        contextRuntimeKeys = contextRuntimeKeys == null ? List.of() : List.copyOf(contextRuntimeKeys);
        parameterRuntimeKeys = parameterRuntimeKeys == null ? List.of() : List.copyOf(parameterRuntimeKeys);
        directiveDispatchReportFields = directiveDispatchReportFields == null
                ? List.of()
                : List.copyOf(directiveDispatchReportFields);
        directiveDispatchSummaryFields = directiveDispatchSummaryFields == null
                ? List.of()
                : List.copyOf(directiveDispatchSummaryFields);
        directiveDispatchAttentionFields = directiveDispatchAttentionFields == null
                ? List.of()
                : List.copyOf(directiveDispatchAttentionFields);
        directiveDispatchRemediationFields = directiveDispatchRemediationFields == null
                ? List.of()
                : List.copyOf(directiveDispatchRemediationFields);
        directiveDispatchRemediationActionFields = directiveDispatchRemediationActionFields == null
                ? List.of()
                : List.copyOf(directiveDispatchRemediationActionFields);
        directiveDispatchResultFields = directiveDispatchResultFields == null
                ? List.of()
                : List.copyOf(directiveDispatchResultFields);
        runtimePortDescriptorFields = runtimePortDescriptorFields == null
                ? List.of()
                : List.copyOf(runtimePortDescriptorFields);
        runtimeEventFields = runtimeEventFields == null ? List.of() : List.copyOf(runtimeEventFields);
    }

    public static HermesMetadataContract current() {
        return new HermesMetadataContract(
                CURRENT_ID,
                CURRENT_SCHEMA_VERSION,
                HermesAgentMode.MODE_ID,
                HermesMetadataKeys.CONTEXT_PLAN_KEYS,
                HermesMetadataKeys.PARAMETER_PLAN_KEYS,
                HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS,
                HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS,
                HermesMetadataKeys.CONTEXT_RUNTIME_KEYS,
                HermesMetadataKeys.PARAMETER_RUNTIME_KEYS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS,
                HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS,
                HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS,
                HermesMetadataKeys.RUNTIME_EVENT_FIELDS);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id);
        metadata.put("schemaVersion", schemaVersion);
        metadata.put("mode", mode);
        metadata.put("contextPlanKeys", contextPlanKeys);
        metadata.put("parameterPlanKeys", parameterPlanKeys);
        metadata.put("contextDirectiveKeys", contextDirectiveKeys);
        metadata.put("parameterDirectiveKeys", parameterDirectiveKeys);
        metadata.put("contextRuntimeKeys", contextRuntimeKeys);
        metadata.put("parameterRuntimeKeys", parameterRuntimeKeys);
        metadata.put("directiveDispatchReportFields", directiveDispatchReportFields);
        metadata.put("directiveDispatchSummaryFields", directiveDispatchSummaryFields);
        metadata.put("directiveDispatchAttentionFields", directiveDispatchAttentionFields);
        metadata.put("directiveDispatchRemediationFields", directiveDispatchRemediationFields);
        metadata.put("directiveDispatchRemediationActionFields", directiveDispatchRemediationActionFields);
        metadata.put("directiveDispatchResultFields", directiveDispatchResultFields);
        metadata.put("runtimePortDescriptorFields", runtimePortDescriptorFields);
        metadata.put("runtimeEventFields", runtimeEventFields);
        return Map.copyOf(metadata);
    }
}
