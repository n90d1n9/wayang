package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.ProductProfile;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangPlatformApi;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangSdkBoundary;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryConfigDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileFileSource;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryPreflightReport;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileRegistryResolution;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationPolicy;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationPolicyDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangPlatformReadinessProfileValidationReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.concurrent.Callable;

final class WayangPlatformCommands {

    private WayangPlatformCommands() {
    }

    @Command(name = "status", description = "Show platform boundary and adapter status.")
    static final class StatusCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render status as a compact JSON object.")
        boolean json;

        @Option(names = "--readiness", description = "Render aggregate production readiness instead of boundary status.")
        boolean readiness;

        @Option(
                names = "--readiness-profile",
                description = "Select readiness profile id from the configured readiness profile registry.")
        String readinessProfile;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangPlatformApi platform = context.client().platform();
                if (readiness || hasReadinessProfile()) {
                    WayangReadinessReport report = hasReadinessProfile()
                            ? platform.readiness(readinessProfile)
                            : platform.readiness();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessJson(report),
                            () -> WayangReadinessTextFormat.text(report));
                    return report.exitCode();
                }
                WayangPlatformStatus status = platform.status();
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> platform.statusJson(status),
                        () -> WayangStatusTextFormat.text(status));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        private boolean hasReadinessProfile() {
            return readinessProfile != null;
        }
    }

    @Command(name = "products", description = "List product surfaces that can be powered by the Wayang agent engine.")
    static final class ProductsCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render product surfaces and policies as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangPlatformApi platform = context.client().platform();
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        platform::productCatalogJson,
                        () -> WayangProductTextFormat.text(
                                platform.productSurfaces(),
                                platform.productSurfacePolicies(),
                                platform.productProfiles()));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(name = "sdk-boundaries", description = "List SDK ownership boundaries for package and API separation.")
    static final class SdkBoundariesCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Parameters(index = "0", arity = "0..1", description = "Optional SDK boundary id to inspect.")
        String boundaryId;

        @Option(names = "--json", description = "Render SDK boundaries as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangPlatformApi platform = context.client().platform();
                if (boundaryId == null || boundaryId.isBlank()) {
                    List<WayangSdkBoundary> boundaries = platform.sdkBoundaries();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            platform::sdkBoundaryCatalogJson,
                            () -> WayangSdkBoundaryTextFormat.text(boundaries));
                    return 0;
                }
                WayangSdkBoundary boundary = platform.sdkBoundary(boundaryId);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> platform.sdkBoundaryJson(boundaryId),
                        () -> WayangSdkBoundaryTextFormat.detailText(boundary));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "readiness-profiles",
            description = "List platform readiness profiles and component bindings.",
            subcommands = {
                    ReadinessProfilesCommand.ConfigCommand.class,
                    ReadinessProfilesCommand.InspectCommand.class,
                    ReadinessProfilesCommand.PoliciesCommand.class,
                    ReadinessProfilesCommand.PreflightCommand.class,
                    ReadinessProfilesCommand.ProvidersCommand.class,
                    ReadinessProfilesCommand.SourcesCommand.class
            })
    static final class ReadinessProfilesCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--json", description = "Render readiness profiles as compact JSON.")
        boolean json;

        @Option(names = "--check", description = "Validate readiness profile bindings instead of listing profiles.")
        boolean check;

        @Option(
                names = "--validation-policy",
                description = "Validation policy id for --check: strict, relaxed, strict-without-profile-roles, strict-without-full-coverage, or relaxed-with-full-coverage.")
        String validationPolicy;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangPlatformApi platform = context.client().platform();
                if (check) {
                    WayangPlatformReadinessProfileValidationReport report = validationPolicy == null
                            ? platform.readinessProfileValidation()
                            : platform.readinessProfileValidation(validationPolicy);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileValidationJson(report),
                            () -> WayangReadinessProfileTextFormat.validationText(report));
                    return report.valid() ? 0 : 1;
                }
                List<WayangPlatformReadinessProfileDescriptor> profiles =
                        platform.readinessProfiles();
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> platform.readinessProfilesJson(profiles),
                        () -> WayangReadinessProfileTextFormat.text(profiles));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        WayangCliContext context() {
            return parent.context();
        }

        @Command(name = "config", description = "Inspect readiness profile registry configuration diagnostics.")
        static final class ConfigCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Option(names = "--json", description = "Render readiness profile registry config diagnostics as JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                            platform.readinessProfileRegistryConfigDiagnostics();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileRegistryConfigDiagnosticsJson(diagnostics),
                            () -> WayangReadinessProfileTextFormat.registryConfigDiagnosticsText(diagnostics));
                    return diagnostics.valid() ? 0 : 1;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "inspect", description = "Inspect one platform readiness profile.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Parameters(index = "0", description = "Readiness profile id.")
            String profileId;

            @Option(names = "--json", description = "Render readiness profile as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    WayangPlatformReadinessProfileDescriptor profile =
                            platform.readinessProfile(profileId);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileDetailJson(profile),
                            () -> WayangReadinessProfileTextFormat.detailText(profile));
                    return 0;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "policies", description = "List readiness profile validation policies.")
        static final class PoliciesCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Option(names = "--json", description = "Render readiness profile validation policies as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies =
                            platform.readinessProfileValidationPolicies();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileValidationPoliciesJson(policies),
                            () -> WayangReadinessProfileTextFormat.validationPoliciesText(policies));
                    return 0;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "preflight", description = "Run production preflight for readiness profile registry sources and providers.")
        static final class PreflightCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Option(names = "--json", description = "Render readiness profile registry preflight as JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    WayangPlatformReadinessProfileRegistryPreflightReport report =
                            platform.readinessProfileRegistryPreflight();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileRegistryPreflightJson(report),
                            () -> WayangReadinessProfileTextFormat.registryPreflightText(report));
                    return report.exitCode();
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "providers", description = "Inspect readiness profile external reader provider discovery.")
        static final class ProvidersCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Option(names = "--json", description = "Render readiness profile external reader provider diagnostics as JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport report =
                            platform.readinessProfileExternalReaderProviderDiscovery();
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileExternalReaderProviderDiscoveryJson(report),
                            () -> WayangReadinessProfileTextFormat.externalReaderProviderDiscoveryText(report));
                    return report.exitCode();
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }

        @Command(name = "sources", description = "Inspect readiness profile registry source resolution.")
        static final class SourcesCommand implements Callable<Integer> {
            @ParentCommand
            ReadinessProfilesCommand parent;

            @Option(names = "--json", description = "Render readiness profile source resolution as compact JSON.")
            boolean json;

            @Option(names = "--file", description = "Resolve a readiness profile properties file with built-in fallback.")
            String file;

            @Option(
                    names = "--validation-policy",
                    description = "Validation policy id for source resolution: strict, relaxed, strict-without-profile-roles, strict-without-full-coverage, or relaxed-with-full-coverage.")
            String validationPolicy;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangPlatformApi platform = context.client().platform();
                    WayangPlatformReadinessProfileRegistryResolution resolution = resolve(platform);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> platform.readinessProfileRegistryResolutionJson(resolution),
                            () -> WayangReadinessProfileTextFormat.registryResolutionText(resolution));
                    return resolution.valid() ? 0 : 1;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }

            private WayangPlatformReadinessProfileRegistryResolution resolve(WayangPlatformApi platform) {
                WayangPlatformReadinessProfileValidationPolicy policy = validationPolicy == null
                        ? null
                        : platform.readinessProfileValidationPolicy(validationPolicy);
                if (file == null || file.isBlank()) {
                    return platform.readinessProfileRegistryResolution(policy);
                }
                return platform.readinessProfileRegistryResolution(
                        WayangPlatformReadinessProfileFileSource.of(file),
                        policy);
            }
        }
    }

    @Command(
            name = "profiles",
            description = "List reusable Wayang product profiles.",
            subcommands = {
                    ProfilesCommand.InspectCommand.class
            })
    static final class ProfilesCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = "--surface", description = "Filter profiles by product surface id.")
        String surfaceId;

        @Option(names = "--json", description = "Render product profiles as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangClient client = context.client();
                WayangPlatformApi platform = client.platform();
                List<ProductProfile> profiles = surfaceId == null || surfaceId.isBlank()
                        ? platform.productProfiles()
                        : platform.productProfilesForSurface(surfaceId);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> platform.profilesJson(surfaceId, profiles),
                        () -> WayangProfileTextFormat.text(client.productName(), surfaceId, profiles));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        WayangCliContext context() {
            return parent.context();
        }

        @Command(name = "inspect", description = "Inspect one reusable Wayang product profile.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand
            ProfilesCommand parent;

            @Parameters(index = "0", description = "Product profile id.")
            String profileId;

            @Option(names = "--json", description = "Render product profile as compact JSON.")
            boolean json;

            @Override
            public Integer call() {
                try {
                    WayangCliContext context = parent.context();
                    WayangClient client = context.client();
                    ProductProfile profile = client.platform().productProfile(profileId);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> client.platform().profileDetailJson(profile),
                            () -> WayangProfileTextFormat.detailText(client.productName(), profile));
                    return 0;
                } catch (RuntimeException e) {
                    return parent.context().commandFailure(e);
                }
            }
        }
    }
}
