package tech.kayys.wayang.agent.planner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;


public final class AgentRunPlanningJsonSchemaProperties {

    private AgentRunPlanningJsonSchemaProperties() {
    }

    static List<String> preflightRequired() {
        return List.of(
                "contract",
                "surfaceId",
                "ready",
                "surfacePolicyAssessment",
                "skillAssessment");
    }

    static List<String> previewRequired() {
        return List.of(
                "contract",
                "requestId",
                "tenantId",
                "modelId",
                "workflowId",
                "surfaceId",
                "sessionId",
                "userId",
                "systemPromptPresent",
                "promptCharacters",
                "systemPromptCharacters",
                "memoryEnabled",
                "maxSteps",
                "workspaceAttached",
                "harnessAttached",
                "skills",
                "contextKeys",
                "context",
                "parameters",
                "surfacePolicyAssessment",
                "skillAssessment");
    }

    static Map<String, Object> preflightProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("ready", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("surfacePolicyAssessment", surfacePolicyAssessmentProperty());
        properties.put("skillAssessment", skillAssessmentProperty());
        return properties;
    }

    static Map<String, Object> previewProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        properties.put("requestId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("tenantId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("modelId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("workflowId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("sessionId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("userId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("systemPromptPresent", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("promptCharacters", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("systemPromptCharacters", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("memoryEnabled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("maxSteps", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("workspaceAttached", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("harnessAttached", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("skills", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("contextKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("context", WayangJsonSchemaDocuments.openObjectProperty());
        properties.put("parameters", WayangJsonSchemaDocuments.openObjectProperty());
        properties.put("surfacePolicyAssessment", surfacePolicyAssessmentProperty());
        properties.put("skillAssessment", skillAssessmentProperty());
        return properties;
    }

    private static Map<String, Object> surfacePolicyAssessmentProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("ready", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("satisfiedContextKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("missingContextKeys", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("recommendations", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("routingHints", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "surfaceId",
                        "ready",
                        "satisfiedContextKeys",
                        "missingContextKeys",
                        "recommendations",
                        "routingHints"),
                true,
                properties);
    }

    private static Map<String, Object> skillAssessmentProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("surfaceId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("ready", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("requestedSkills", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("resolvedSkillIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("unknownSkills", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("unavailableSkillIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("incompatibleSkillIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("recommendations", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "surfaceId",
                        "ready",
                        "requestedSkills",
                        "resolvedSkillIds",
                        "unknownSkills",
                        "unavailableSkillIds",
                        "incompatibleSkillIds",
                        "recommendations"),
                true,
                properties);
    }
}
