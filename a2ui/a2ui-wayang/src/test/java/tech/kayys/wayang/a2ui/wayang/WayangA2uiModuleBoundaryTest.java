package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.action.ActionBindingReportProjection;
import tech.kayys.wayang.a2ui.wayang.action.ActionContextReader;
import tech.kayys.wayang.a2ui.wayang.action.ActionGate;
import tech.kayys.wayang.a2ui.wayang.action.ActionMetadata;
import tech.kayys.wayang.a2ui.wayang.action.ActionQueries;
import tech.kayys.wayang.a2ui.wayang.action.ActionResponses;
import tech.kayys.wayang.a2ui.wayang.http.HttpActionBindingProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpActionBindingProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpBindingReportProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticPlanProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointDiagnosticProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpEndpointProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpExpectationProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpExchangeMetrics;
import tech.kayys.wayang.a2ui.wayang.http.HttpHeaderValues;
import tech.kayys.wayang.a2ui.wayang.http.HttpIssueMaps;
import tech.kayys.wayang.a2ui.wayang.http.HttpMetricExchange;
import tech.kayys.wayang.a2ui.wayang.http.HttpOperationalDiagnosticsProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpPublicationProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpReadinessProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpReadinessProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.http.HttpReportMetricDecoders;
import tech.kayys.wayang.a2ui.wayang.http.HttpReportMetrics;
import tech.kayys.wayang.a2ui.wayang.http.HttpResponseBodyDecoder;
import tech.kayys.wayang.a2ui.wayang.http.HttpRouteProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpScenarioSuiteMetrics;
import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeProjection;
import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeProbeResponseDecoder;
import tech.kayys.wayang.a2ui.wayang.http.HttpSmokeResultProjection;
import tech.kayys.wayang.a2ui.wayang.projection.SessionProjection;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLookupProvider;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadAttempt;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResult;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadResultDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigLoadStatus;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigObjectStorageProvider;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestContext;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsSummary;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigRequestDiagnosticsSummaryDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSource;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceCapability;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceDiagnostics;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceDiagnosticsDecoder;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourcePolicy;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceProvider;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceRedactor;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceRegistries;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceRegistry;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceSpec;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSourceSpecs;
import tech.kayys.wayang.a2ui.wayang.session.SessionConfigSources;
import tech.kayys.wayang.a2ui.wayang.session.SessionProfiles;
import tech.kayys.wayang.a2ui.wayang.surface.RunActionControls;
import tech.kayys.wayang.a2ui.wayang.surface.RunDataModels;
import tech.kayys.wayang.a2ui.wayang.surface.RunEventsSurface;
import tech.kayys.wayang.a2ui.wayang.surface.RunHistorySurface;
import tech.kayys.wayang.a2ui.wayang.surface.RunInspectionSurface;
import tech.kayys.wayang.a2ui.wayang.surface.RunStatusSurface;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceActions;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceData;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceIds;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceLayouts;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceMessages;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceProjection;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceText;
import tech.kayys.wayang.a2ui.wayang.transport.TransportJson;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMetadataProjection;
import tech.kayys.wayang.a2ui.wayang.transport.TransportExchangeMetrics;
import tech.kayys.wayang.a2ui.wayang.transport.TransportMetricExchange;
import tech.kayys.wayang.a2ui.wayang.transport.TransportProjection;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiModuleBoundaryTest {

    @Test
    void namesTargetPackageSkeleton() {
        assertThat(WayangA2uiModuleBoundary.basePackage())
                .isEqualTo("tech.kayys.wayang.a2ui.wayang");
        assertThat(WayangA2uiModuleBoundary.targetSubpackages())
                .extracting(WayangA2uiModuleBoundary::packageName)
                .containsExactly(
                        "tech.kayys.wayang.a2ui.wayang.action",
                        "tech.kayys.wayang.a2ui.wayang.bridge",
                        "tech.kayys.wayang.a2ui.wayang.http",
                        "tech.kayys.wayang.a2ui.wayang.projection",
                        "tech.kayys.wayang.a2ui.wayang.session",
                        "tech.kayys.wayang.a2ui.wayang.spec",
                        "tech.kayys.wayang.a2ui.wayang.surface",
                        "tech.kayys.wayang.a2ui.wayang.transport",
                        "tech.kayys.wayang.a2ui.wayang.support");
    }

    @Test
    void exposesUniqueBoundaryLabelsAndResponsibilities() {
        assertThat(WayangA2uiModuleBoundary.values())
                .extracting(WayangA2uiModuleBoundary::label)
                .doesNotHaveDuplicates();
        assertThat(WayangA2uiModuleBoundary.values())
                .allSatisfy(boundary -> assertThat(boundary.responsibility()).isNotBlank());
        assertThat(WayangA2uiModuleBoundary.FACADE.rootPackage()).isTrue();
        assertThat(WayangA2uiModuleBoundary.HTTP.targetSubpackage()).isTrue();
    }

    @Test
    void publishesPackageDescriptorsForEachBoundary() {
        assertThat(WayangA2uiModuleBoundary.values())
                .allSatisfy(boundary -> assertThat(packageDescriptor(boundary))
                        .as(boundary.packageName())
                        .isNotNull());
    }

    @Test
    void keepsPublicApiBrandedAndLocalHelpersCompact() {
        assertThat(WayangA2ui.class.getSimpleName()).startsWith("WayangA2ui");
        assertThat(ActionBindingReportProjection.class.getSimpleName())
                .isEqualTo("ActionBindingReportProjection");
        assertThat(ActionContextReader.class.getSimpleName()).isEqualTo("ActionContextReader");
        assertThat(ActionGate.class.getSimpleName()).isEqualTo("ActionGate");
        assertThat(ActionMetadata.class.getSimpleName()).isEqualTo("ActionMetadata");
        assertThat(ActionQueries.class.getSimpleName()).isEqualTo("ActionQueries");
        assertThat(ActionResponses.class.getSimpleName()).isEqualTo("ActionResponses");
        assertThat(HttpActionBindingProbeProjection.class.getSimpleName())
                .isEqualTo("HttpActionBindingProbeProjection");
        assertThat(HttpActionBindingProbeResponseDecoder.class.getSimpleName())
                .isEqualTo("HttpActionBindingProbeResponseDecoder");
        assertThat(HttpBindingReportProjection.class.getSimpleName()).isEqualTo("HttpBindingReportProjection");
        assertThat(HttpBindingReportProbeProjection.class.getSimpleName())
                .isEqualTo("HttpBindingReportProbeProjection");
        assertThat(HttpBindingReportProbeResponseDecoder.class.getSimpleName())
                .isEqualTo("HttpBindingReportProbeResponseDecoder");
        assertThat(HttpEndpointDiagnosticPlanProjection.class.getSimpleName())
                .isEqualTo("HttpEndpointDiagnosticPlanProjection");
        assertThat(HttpEndpointDiagnosticProjection.class.getSimpleName())
                .isEqualTo("HttpEndpointDiagnosticProjection");
        assertThat(HttpEndpointProjection.class.getSimpleName()).isEqualTo("HttpEndpointProjection");
        assertThat(HttpExpectationProjection.class.getSimpleName()).isEqualTo("HttpExpectationProjection");
        assertThat(HttpExchangeMetrics.class.getSimpleName()).isEqualTo("HttpExchangeMetrics");
        assertThat(HttpHeaderValues.class.getSimpleName()).isEqualTo("HttpHeaderValues");
        assertThat(HttpMetricExchange.class.getSimpleName()).isEqualTo("HttpMetricExchange");
        assertThat(HttpOperationalDiagnosticsProjection.class.getSimpleName())
                .isEqualTo("HttpOperationalDiagnosticsProjection");
        assertThat(HttpPublicationProjection.class.getSimpleName()).isEqualTo("HttpPublicationProjection");
        assertThat(HttpReadinessProbeProjection.class.getSimpleName())
                .isEqualTo("HttpReadinessProbeProjection");
        assertThat(HttpReadinessProbeResponseDecoder.class.getSimpleName())
                .isEqualTo("HttpReadinessProbeResponseDecoder");
        assertThat(HttpReportMetricDecoders.class.getSimpleName()).isEqualTo("HttpReportMetricDecoders");
        assertThat(HttpReportMetrics.class.getSimpleName()).isEqualTo("HttpReportMetrics");
        assertThat(HttpResponseBodyDecoder.class.getSimpleName()).isEqualTo("HttpResponseBodyDecoder");
        assertThat(HttpRouteProjection.class.getSimpleName()).isEqualTo("HttpRouteProjection");
        assertThat(HttpScenarioProjection.class.getSimpleName()).isEqualTo("HttpScenarioProjection");
        assertThat(HttpScenarioSuiteMetrics.class.getSimpleName()).isEqualTo("HttpScenarioSuiteMetrics");
        assertThat(HttpSmokeProbeProjection.class.getSimpleName()).isEqualTo("HttpSmokeProbeProjection");
        assertThat(HttpSmokeProbeResponseDecoder.class.getSimpleName()).isEqualTo("HttpSmokeProbeResponseDecoder");
        assertThat(HttpSmokeResultProjection.class.getSimpleName()).isEqualTo("HttpSmokeResultProjection");
        assertThat(TransportMaps.class.getSimpleName()).isEqualTo("TransportMaps");
        assertThat(TransportJson.class.getSimpleName()).isEqualTo("TransportJson");
        assertThat(TransportExchangeMetrics.class.getSimpleName()).isEqualTo("TransportExchangeMetrics");
        assertThat(TransportMetricExchange.class.getSimpleName()).isEqualTo("TransportMetricExchange");
        assertThat(TransportMetadataProjection.class.getSimpleName()).isEqualTo("TransportMetadataProjection");
        assertThat(TransportProjection.class.getSimpleName()).isEqualTo("TransportProjection");
        assertThat(HttpIssueMaps.class.getSimpleName()).isEqualTo("HttpIssueMaps");
        assertThat(SessionProjection.class.getSimpleName()).isEqualTo("SessionProjection");
        assertThat(SessionConfigDecoder.class.getSimpleName()).isEqualTo("SessionConfigDecoder");
        assertThat(SessionConfigLookupProvider.class.getSimpleName()).isEqualTo("SessionConfigLookupProvider");
        assertThat(SessionConfigLoadAttempt.class.getSimpleName()).isEqualTo("SessionConfigLoadAttempt");
        assertThat(SessionConfigLoadResult.class.getSimpleName()).isEqualTo("SessionConfigLoadResult");
        assertThat(SessionConfigLoadResultDecoder.class.getSimpleName()).isEqualTo("SessionConfigLoadResultDecoder");
        assertThat(SessionConfigLoadStatus.class.getSimpleName()).isEqualTo("SessionConfigLoadStatus");
        assertThat(SessionConfigObjectStorageProvider.class.getSimpleName())
                .isEqualTo("SessionConfigObjectStorageProvider");
        assertThat(SessionConfigRequestContext.class.getSimpleName()).isEqualTo("SessionConfigRequestContext");
        assertThat(SessionConfigRequestDiagnostics.class.getSimpleName())
                .isEqualTo("SessionConfigRequestDiagnostics");
        assertThat(SessionConfigRequestDiagnosticsDecoder.class.getSimpleName())
                .isEqualTo("SessionConfigRequestDiagnosticsDecoder");
        assertThat(SessionConfigRequestDiagnosticsSummary.class.getSimpleName())
                .isEqualTo("SessionConfigRequestDiagnosticsSummary");
        assertThat(SessionConfigRequestDiagnosticsSummaryDecoder.class.getSimpleName())
                .isEqualTo("SessionConfigRequestDiagnosticsSummaryDecoder");
        assertThat(SessionConfigSource.class.getSimpleName()).isEqualTo("SessionConfigSource");
        assertThat(SessionConfigSourceCapability.class.getSimpleName()).isEqualTo("SessionConfigSourceCapability");
        assertThat(SessionConfigSourceDiagnostics.class.getSimpleName())
                .isEqualTo("SessionConfigSourceDiagnostics");
        assertThat(SessionConfigSourceDiagnosticsDecoder.class.getSimpleName())
                .isEqualTo("SessionConfigSourceDiagnosticsDecoder");
        assertThat(SessionConfigSourcePolicy.class.getSimpleName()).isEqualTo("SessionConfigSourcePolicy");
        assertThat(SessionConfigSourceProvider.class.getSimpleName()).isEqualTo("SessionConfigSourceProvider");
        assertThat(SessionConfigSourceRedactor.class.getSimpleName()).isEqualTo("SessionConfigSourceRedactor");
        assertThat(SessionConfigSourceRegistries.class.getSimpleName()).isEqualTo("SessionConfigSourceRegistries");
        assertThat(SessionConfigSourceRegistry.class.getSimpleName()).isEqualTo("SessionConfigSourceRegistry");
        assertThat(SessionConfigSourceSpec.class.getSimpleName()).isEqualTo("SessionConfigSourceSpec");
        assertThat(SessionConfigSourceSpecs.class.getSimpleName()).isEqualTo("SessionConfigSourceSpecs");
        assertThat(SessionConfigSources.class.getSimpleName()).isEqualTo("SessionConfigSources");
        assertThat(SessionProfiles.class.getSimpleName()).isEqualTo("SessionProfiles");
        assertThat(RunActionControls.class.getSimpleName()).isEqualTo("RunActionControls");
        assertThat(RunDataModels.class.getSimpleName()).isEqualTo("RunDataModels");
        assertThat(RunEventsSurface.class.getSimpleName()).isEqualTo("RunEventsSurface");
        assertThat(RunHistorySurface.class.getSimpleName()).isEqualTo("RunHistorySurface");
        assertThat(RunInspectionSurface.class.getSimpleName()).isEqualTo("RunInspectionSurface");
        assertThat(RunStatusSurface.class.getSimpleName()).isEqualTo("RunStatusSurface");
        assertThat(SurfaceActions.class.getSimpleName()).isEqualTo("SurfaceActions");
        assertThat(SurfaceData.class.getSimpleName()).isEqualTo("SurfaceData");
        assertThat(SurfaceIds.class.getSimpleName()).isEqualTo("SurfaceIds");
        assertThat(SurfaceLayouts.class.getSimpleName()).isEqualTo("SurfaceLayouts");
        assertThat(SurfaceMessages.class.getSimpleName()).isEqualTo("SurfaceMessages");
        assertThat(SurfaceProjection.class.getSimpleName()).isEqualTo("SurfaceProjection");
        assertThat(SurfaceText.class.getSimpleName()).isEqualTo("SurfaceText");
    }

    private static Class<?> packageDescriptor(WayangA2uiModuleBoundary boundary) {
        try {
            return Class.forName(boundary.packageName() + ".package-info");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
