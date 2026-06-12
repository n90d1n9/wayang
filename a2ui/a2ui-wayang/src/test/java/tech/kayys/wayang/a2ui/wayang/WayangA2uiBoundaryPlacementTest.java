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
import tech.kayys.wayang.a2ui.wayang.projection.SpecAlignmentProjection;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiBoundaryPlacementTest {

    @Test
    void namesSupportFirstMigrationCandidates() {
        assertThat(WayangA2uiBoundaryPlacement.supportFirstCandidates())
                .containsExactly(
                        "DecodeCollections",
                        "DecodeValues",
                        "ProjectionCollections",
                        "RecordCollections",
                        "RecordMaps",
                        "RecordNumbers",
                        "RecordValues",
                        "StringMaps");
        assertThat(WayangA2uiBoundaryPlacement.supportFirstCandidates())
                .allSatisfy(candidate -> assertThat(WayangA2uiBoundaryPlacement.classifySimpleName(candidate))
                        .isEqualTo(WayangA2uiModuleBoundary.SUPPORT));
    }

    @Test
    void classifiesProjectionHelpersBeforeDomainWords() {
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.PROJECTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SpecAlignmentProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.PROJECTION);
        assertThat(WayangA2uiBoundaryPlacement.classifySimpleName("ProjectionMaps"))
                .isEqualTo(WayangA2uiModuleBoundary.PROJECTION);
    }

    @Test
    void classifiesRepresentativePublicContracts() {
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2ui.class))
                .isEqualTo(WayangA2uiModuleBoundary.FACADE);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiActionPolicy.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionBindingReportProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionContextReader.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionGate.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionMetadata.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionQueries.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(ActionResponses.class))
                .isEqualTo(WayangA2uiModuleBoundary.ACTION);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiBridgeRequest.class))
                .isEqualTo(WayangA2uiModuleBoundary.BRIDGE);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiHttpScenarioSuite.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiSessionConfig.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigLookupProvider.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigLoadAttempt.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigLoadResult.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigLoadResultDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigLoadStatus.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigObjectStorageProvider.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigRequestContext.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigRequestDiagnostics.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigRequestDiagnosticsDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigRequestDiagnosticsSummary.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigRequestDiagnosticsSummaryDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSource.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceCapability.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceDiagnostics.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceDiagnosticsDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourcePolicy.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceProvider.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceRedactor.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceRegistries.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceRegistry.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceSpec.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSourceSpecs.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionConfigSources.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(SessionProfiles.class))
                .isEqualTo(WayangA2uiModuleBoundary.SESSION);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiSpecAlignmentReport.class))
                .isEqualTo(WayangA2uiModuleBoundary.SPEC);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiSurfaceCatalog.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunActionControls.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunDataModels.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunEventsSurface.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunHistorySurface.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunInspectionSurface.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(RunStatusSurface.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceActions.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceData.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceIds.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceLayouts.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceMessages.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(SurfaceText.class))
                .isEqualTo(WayangA2uiModuleBoundary.SURFACE);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiTransportRequest.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
    }

    @Test
    void keepsHttpModelsInsideHttpEvenWhenNamesMentionActionOrTransport() {
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiHttpActionBindingProbeResult.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(WayangA2uiHttpResponse.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpActionBindingProbeProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpActionBindingProbeResponseDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpBindingReportProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpBindingReportProbeProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpBindingReportProbeResponseDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpEndpointDiagnosticPlanProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpEndpointDiagnosticProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpEndpointProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpExpectationProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpExchangeMetrics.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpHeaderValues.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpIssueMaps.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpMetricExchange.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpOperationalDiagnosticsProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpPublicationProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpReadinessProbeProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpReadinessProbeResponseDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpReportMetricDecoders.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpReportMetrics.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpResponseBodyDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpRouteProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpScenarioProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpScenarioSuiteMetrics.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpSmokeProbeProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpSmokeProbeResponseDecoder.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
        assertThat(WayangA2uiBoundaryPlacement.classify(HttpSmokeResultProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.HTTP);
    }

    @Test
    void classifiesMovedTransportHelpersFromTheTransportPackage() {
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportMaps.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportJson.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportExchangeMetrics.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportMetricExchange.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportMetadataProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
        assertThat(WayangA2uiBoundaryPlacement.classify(TransportProjection.class))
                .isEqualTo(WayangA2uiModuleBoundary.TRANSPORT);
    }

    @Test
    void rejectsUnknownOrBlankClassNames() {
        assertThatThrownBy(() -> WayangA2uiBoundaryPlacement.classifySimpleName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> WayangA2uiBoundaryPlacement.classifySimpleName("UnknownHelper"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No A2UI module boundary placement");
    }
}
