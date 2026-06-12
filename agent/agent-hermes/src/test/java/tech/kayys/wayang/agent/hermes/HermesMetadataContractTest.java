package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesMetadataContractTest {

    @Test
    void currentContractRendersVersionedMetadataEnvelope() {
        HermesMetadataContract contract = HermesMetadataContract.current();

        assertThat(contract.id()).isEqualTo(HermesMetadataContract.CURRENT_ID);
        assertThat(contract.schemaVersion()).isEqualTo(HermesMetadataContract.CURRENT_SCHEMA_VERSION);
        assertThat(contract.mode()).isEqualTo(HermesAgentMode.MODE_ID);
        assertThat(contract.contextPlanKeys()).containsExactlyElementsOf(HermesMetadataKeys.CONTEXT_PLAN_KEYS);
        assertThat(contract.parameterPlanKeys()).containsExactlyElementsOf(HermesMetadataKeys.PARAMETER_PLAN_KEYS);
        assertThat(contract.contextDirectiveKeys()).containsExactlyElementsOf(HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS);
        assertThat(contract.parameterDirectiveKeys())
                .containsExactlyElementsOf(HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS);
        assertThat(contract.contextRuntimeKeys()).containsExactlyElementsOf(HermesMetadataKeys.CONTEXT_RUNTIME_KEYS);
        assertThat(contract.parameterRuntimeKeys())
                .containsExactlyElementsOf(HermesMetadataKeys.PARAMETER_RUNTIME_KEYS);
        assertThat(contract.directiveDispatchReportFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS);
        assertThat(contract.directiveDispatchSummaryFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS);
        assertThat(contract.directiveDispatchAttentionFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS);
        assertThat(contract.directiveDispatchRemediationFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS);
        assertThat(contract.directiveDispatchRemediationActionFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS);
        assertThat(contract.directiveDispatchResultFields())
                .containsExactlyElementsOf(HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS);
        assertThat(contract.runtimePortDescriptorFields())
                .containsExactlyElementsOf(HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS);
        assertThat(contract.runtimeEventFields())
                .containsExactlyElementsOf(HermesMetadataKeys.RUNTIME_EVENT_FIELDS);
        assertThat(contract.toMetadata())
                .containsEntry("id", HermesMetadataContract.CURRENT_ID)
                .containsEntry("schemaVersion", HermesMetadataContract.CURRENT_SCHEMA_VERSION)
                .containsEntry("mode", HermesAgentMode.MODE_ID)
                .containsEntry("contextPlanKeys", HermesMetadataKeys.CONTEXT_PLAN_KEYS)
                .containsEntry("parameterPlanKeys", HermesMetadataKeys.PARAMETER_PLAN_KEYS)
                .containsEntry("contextDirectiveKeys", HermesMetadataKeys.CONTEXT_DIRECTIVE_KEYS)
                .containsEntry("parameterDirectiveKeys", HermesMetadataKeys.PARAMETER_DIRECTIVE_KEYS)
                .containsEntry("contextRuntimeKeys", HermesMetadataKeys.CONTEXT_RUNTIME_KEYS)
                .containsEntry("parameterRuntimeKeys", HermesMetadataKeys.PARAMETER_RUNTIME_KEYS)
                .containsEntry("directiveDispatchReportFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_REPORT_FIELDS)
                .containsEntry("directiveDispatchSummaryFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_SUMMARY_FIELDS)
                .containsEntry("directiveDispatchAttentionFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_ATTENTION_FIELDS)
                .containsEntry("directiveDispatchRemediationFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_FIELDS)
                .containsEntry("directiveDispatchRemediationActionFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_REMEDIATION_ACTION_FIELDS)
                .containsEntry("directiveDispatchResultFields",
                        HermesMetadataKeys.DIRECTIVE_DISPATCH_RESULT_FIELDS)
                .containsEntry("runtimePortDescriptorFields",
                        HermesMetadataKeys.RUNTIME_PORT_DESCRIPTOR_FIELDS)
                .containsEntry("runtimeEventFields",
                        HermesMetadataKeys.RUNTIME_EVENT_FIELDS);
    }

    @Test
    void normalizesBlankContractIdentityToCurrentDefaults() {
        HermesMetadataContract contract = new HermesMetadataContract(
                " ",
                7,
                " ",
                null,
                List.of("customPlan"),
                null,
                List.of("customDirective"),
                null,
                List.of("customRuntime"),
                null,
                List.of("customSummaryField"),
                List.of("customAttentionField"),
                List.of("customRemediationField"),
                List.of("customRemediationActionField"),
                null,
                List.of("customRuntimePortField"),
                List.of("customRuntimeEventField"));

        assertThat(contract.id()).isEqualTo(HermesMetadataContract.CURRENT_ID);
        assertThat(contract.schemaVersion()).isEqualTo(7);
        assertThat(contract.mode()).isEqualTo(HermesAgentMode.MODE_ID);
        assertThat(contract.contextPlanKeys()).isEmpty();
        assertThat(contract.parameterPlanKeys()).containsExactly("customPlan");
        assertThat(contract.contextDirectiveKeys()).isEmpty();
        assertThat(contract.parameterDirectiveKeys()).containsExactly("customDirective");
        assertThat(contract.contextRuntimeKeys()).isEmpty();
        assertThat(contract.parameterRuntimeKeys()).containsExactly("customRuntime");
        assertThat(contract.directiveDispatchReportFields()).isEmpty();
        assertThat(contract.directiveDispatchSummaryFields()).containsExactly("customSummaryField");
        assertThat(contract.directiveDispatchAttentionFields()).containsExactly("customAttentionField");
        assertThat(contract.directiveDispatchRemediationFields()).containsExactly("customRemediationField");
        assertThat(contract.directiveDispatchRemediationActionFields())
                .containsExactly("customRemediationActionField");
        assertThat(contract.directiveDispatchResultFields()).isEmpty();
        assertThat(contract.runtimePortDescriptorFields()).containsExactly("customRuntimePortField");
        assertThat(contract.runtimeEventFields()).containsExactly("customRuntimeEventField");
    }
}
