package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiBoundaryCoverageTest {

    @Test
    void classifiesEveryCurrentSourceType() throws IOException {
        assertThat(sourceTypes())
                .isNotEmpty()
                .allSatisfy(type -> assertThat(WayangA2uiBoundaryPlacement.classify(type))
                        .as(type.getSimpleName())
                        .isNotNull());
    }

    @Test
    void keepsCompactRootHelperNamesPackagePrivateUntilBoundaryMoveIsExplicit() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.basePackage()))
                .filteredOn(type -> !type.getSimpleName().startsWith("WayangA2ui"))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isFalse());
    }

    @Test
    void exposesMovedSupportHelpersFromTheSupportBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SUPPORT.packageName()))
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrderElementsOf(WayangA2uiBoundaryPlacement.supportFirstCandidates());
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SUPPORT.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void exposesMovedProjectionSeedFromTheProjectionBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.PROJECTION.packageName()))
                .extracting(Class::getSimpleName)
                .contains("ProjectionMaps", "SessionProjection", "SpecAlignmentProjection");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.PROJECTION.packageName()))
                .filteredOn(type -> type.getSimpleName().endsWith("Projection"))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
        assertThat(sourceTypes())
                .filteredOn(type -> type.getSimpleName().equals("ProjectionMaps"))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isFalse());
    }

    @Test
    void exposesMovedTransportHelpersFromTheTransportBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.TRANSPORT.packageName()))
                .extracting(Class::getSimpleName)
                .contains(
                        "TransportJson",
                        "TransportMaps",
                        "TransportExchangeMetrics",
                        "TransportMetricExchange",
                        "TransportMetadataProjection",
                        "TransportProjection");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.TRANSPORT.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void exposesMovedActionHelpersFromTheActionBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.ACTION.packageName()))
                .extracting(Class::getSimpleName)
                .contains(
                        "ActionBindingReportProjection",
                        "ActionContextReader",
                        "ActionGate",
                        "ActionMetadata",
                        "ActionQueries",
                        "ActionResponses");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.ACTION.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void exposesMovedSessionHelpersFromTheSessionBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SESSION.packageName()))
                .extracting(Class::getSimpleName)
                .contains(
                        "SessionConfigDecoder",
                        "SessionConfigLookupProvider",
                        "SessionConfigLoadAttempt",
                        "SessionConfigLoadResult",
                        "SessionConfigLoadResultDecoder",
                        "SessionConfigLoadStatus",
                        "SessionConfigObjectStorageProvider",
                        "SessionConfigRequestContext",
                        "SessionConfigRequestDiagnostics",
                        "SessionConfigRequestDiagnosticsDecoder",
                        "SessionConfigRequestDiagnosticsSummary",
                        "SessionConfigRequestDiagnosticsSummaryDecoder",
                        "SessionConfigSource",
                        "SessionConfigSourceCapability",
                        "SessionConfigSourceDiagnostics",
                        "SessionConfigSourceDiagnosticsDecoder",
                        "SessionConfigSourcePolicy",
                        "SessionConfigSourceProvider",
                        "SessionConfigSourceRedactor",
                        "SessionConfigSourceRegistries",
                        "SessionConfigSourceRegistry",
                        "SessionConfigSourceSpec",
                        "SessionConfigSourceSpecs",
                        "SessionConfigSources",
                        "SessionProfiles");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SESSION.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void exposesMovedSurfaceHelpersFromTheSurfaceBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SURFACE.packageName()))
                .extracting(Class::getSimpleName)
                .contains(
                        "RunActionControls",
                        "RunDataModels",
                        "RunEventsSurface",
                        "RunHistorySurface",
                        "RunInspectionSurface",
                        "RunStatusSurface",
                        "SurfaceActions",
                        "SurfaceData",
                        "SurfaceIds",
                        "SurfaceLayouts",
                        "SurfaceMessages",
                        "SurfaceProjection",
                        "SurfaceText");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.SURFACE.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void exposesMovedHttpHelpersFromTheHttpBoundary() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.HTTP.packageName()))
                .extracting(Class::getSimpleName)
                .contains(
                        "HttpActionBindingProbeProjection",
                        "HttpActionBindingProbeResponseDecoder",
                        "HttpBindingReportProjection",
                        "HttpBindingReportProbeProjection",
                        "HttpBindingReportProbeResponseDecoder",
                        "HttpEndpointDiagnosticPlanProjection",
                        "HttpEndpointDiagnosticProjection",
                        "HttpEndpointProjection",
                        "HttpExpectationProjection",
                        "HttpExchangeMetrics",
                        "HttpHeaderValues",
                        "HttpIssueMaps",
                        "HttpMetricExchange",
                        "HttpOperationalDiagnosticsProjection",
                        "HttpPublicationProjection",
                        "HttpReadinessProbeProjection",
                        "HttpReadinessProbeResponseDecoder",
                        "HttpReportMetricDecoders",
                        "HttpReportMetrics",
                        "HttpResponseBodyDecoder",
                        "HttpRouteProjection",
                        "HttpScenarioProjection",
                        "HttpScenarioSuiteMetrics",
                        "HttpSmokeProbeProjection",
                        "HttpSmokeProbeResponseDecoder",
                        "HttpSmokeResultProjection");
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.HTTP.packageName()))
                .allSatisfy(type -> assertThat(Modifier.isPublic(type.getModifiers()))
                        .as(type.getSimpleName())
                        .isTrue());
    }

    @Test
    void keepsCurrentRootPublicSurfaceBranded() throws IOException {
        assertThat(sourceTypes())
                .filteredOn(type -> type.getPackageName().equals(WayangA2uiModuleBoundary.basePackage()))
                .filteredOn(type -> Modifier.isPublic(type.getModifiers()))
                .extracting(Class::getSimpleName)
                .allSatisfy(name -> assertThat(name).startsWith("WayangA2ui"));
    }

    private static List<Class<?>> sourceTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java/tech/kayys/wayang/a2ui/wayang");
        try (var files = Files.walk(sourceRoot)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .map(path -> className(sourceRoot, path))
                    .sorted()
                    .map(WayangA2uiBoundaryCoverageTest::loadType)
                    .toList();
        }
    }

    private static String className(Path sourceRoot, Path path) {
        String relative = sourceRoot.relativize(path).toString();
        String className = relative.substring(0, relative.length() - ".java".length())
                .replace(path.getFileSystem().getSeparator(), ".");
        return WayangA2uiModuleBoundary.basePackage() + "." + className;
    }

    private static Class<?> loadType(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load A2UI type " + className, e);
        }
    }
}
