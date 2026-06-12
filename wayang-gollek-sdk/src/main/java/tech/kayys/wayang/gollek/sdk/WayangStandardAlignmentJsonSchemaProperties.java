package tech.kayys.wayang.gollek.sdk;

import java.util.Map;

final class WayangStandardAlignmentJsonSchemaProperties {

    private WayangStandardAlignmentJsonSchemaProperties() {
    }

    static Map<String, Object> properties() {
        return WayangJsonSchemaObjectBuilder.create()
                .property("product", WayangJsonSchemaDocuments.stringProperty())
                .property("health", healthProperty())
                .properties();
    }

    private static Map<String, Object> healthProperty() {
        return WayangJsonSchemaObjectBuilder.create()
                .required("reportId", WayangJsonSchemaDocuments.stringProperty())
                .required("status", WayangJsonSchemaDocuments.stringProperty())
                .required("ready", WayangJsonSchemaDocuments.booleanProperty())
                .required("aligned", WayangJsonSchemaDocuments.booleanProperty())
                .required("standardCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("gapCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("standardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("gapStandardIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("portfolio", WayangStandardAlignmentPortfolioJsonSchemaProperties.portfolioProperty())
                .required("policyAssessment", WayangStandardAlignmentPolicyJsonSchemaProperties.policyAssessmentProperty())
                .required(
                        "providerPolicyAssessment",
                        WayangStandardAlignmentPolicyJsonSchemaProperties.providerPolicyAssessmentProperty())
                .required("registryDrift", WayangStandardRegistryDriftJsonSchemaProperties.registryDriftProperty())
                .required("registryDriftMode", WayangJsonSchemaDocuments.stringProperty())
                .required(
                        "providerDiagnostics",
                        WayangStandardAlignmentProviderJsonSchemaProperties.providerDiagnosticsProperty())
                .required("providerCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("providerIds", WayangJsonSchemaDocuments.stringArrayProperty())
                .required("providers", WayangJsonSchemaDocuments.arrayProperty(
                        WayangStandardAlignmentProviderJsonSchemaProperties.providerSummaryProperty()))
                .required("providerIssueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty())
                .required("providerIssues", WayangJsonSchemaDocuments.arrayProperty(
                        WayangStandardAlignmentProviderJsonSchemaProperties.providerIssueProperty()))
                .required("recommendations", WayangJsonSchemaDocuments.stringArrayProperty())
                .objectProperty();
    }
}
