package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.List;

public final class WayangPlatformReadinessProfileRegistry {

    private final WayangPlatformReadinessProfileSource primarySource;
    private final WayangPlatformReadinessProfileSource fallbackSource;
    private final WayangPlatformReadinessProfileValidationPolicy validationPolicy;

    private WayangPlatformReadinessProfileRegistry(
            WayangPlatformReadinessProfileSource primarySource,
            WayangPlatformReadinessProfileSource fallbackSource,
            WayangPlatformReadinessProfileValidationPolicy validationPolicy) {
        this.primarySource = primarySource == null
                ? WayangPlatformReadinessProfileBuiltInSource.create()
                : primarySource;
        this.fallbackSource = fallbackSource;
        this.validationPolicy = validationPolicy == null
                ? WayangPlatformReadinessProfileValidationPolicy.defaultPolicy()
                : validationPolicy;
    }

    public static WayangPlatformReadinessProfileRegistry defaultRegistry() {
        return of(WayangPlatformReadinessProfileBuiltInSource.create(), null);
    }

    public static WayangPlatformReadinessProfileRegistry of(
            WayangPlatformReadinessProfileSource primarySource) {
        return of(primarySource, null);
    }

    public static WayangPlatformReadinessProfileRegistry of(
            WayangPlatformReadinessProfileSource primarySource,
            WayangPlatformReadinessProfileSource fallbackSource) {
        return of(primarySource, fallbackSource, WayangPlatformReadinessProfileValidationPolicy.defaultPolicy());
    }

    public static WayangPlatformReadinessProfileRegistry of(
            WayangPlatformReadinessProfileSource primarySource,
            WayangPlatformReadinessProfileSource fallbackSource,
            WayangPlatformReadinessProfileValidationPolicy validationPolicy) {
        return new WayangPlatformReadinessProfileRegistry(
                primarySource,
                fallbackSource,
                validationPolicy);
    }

    public static WayangPlatformReadinessProfileRegistry withBuiltInFallback(
            WayangPlatformReadinessProfileSource primarySource) {
        return of(primarySource, WayangPlatformReadinessProfileBuiltInSource.create());
    }

    public WayangPlatformReadinessProfileRegistryResolution resolve() {
        return resolve(validationPolicy);
    }

    public WayangPlatformReadinessProfileRegistryResolution resolve(
            WayangPlatformReadinessProfileValidationPolicy policy) {
        WayangPlatformReadinessProfileValidationPolicy resolvedPolicy = policy == null
                ? validationPolicy
                : policy;
        List<WayangPlatformReadinessProfileSourceStatus> statuses = new ArrayList<>();
        SourceEvaluation primary = evaluate(primarySource, resolvedPolicy, false);
        boolean usePrimary = primary.result().available() && primary.validation().valid();

        if (usePrimary || fallbackSource == null) {
            statuses.add(status(primary, true));
            return resolution(primary, false, statuses);
        }

        statuses.add(status(primary, false));
        SourceEvaluation fallback = evaluate(fallbackSource, resolvedPolicy, true);
        statuses.add(status(fallback, true));
        return resolution(fallback, true, statuses);
    }

    private static SourceEvaluation evaluate(
            WayangPlatformReadinessProfileSource source,
            WayangPlatformReadinessProfileValidationPolicy policy,
            boolean fallback) {
        WayangPlatformReadinessProfileSourceResult result = source == null
                ? WayangPlatformReadinessProfileBuiltInSource.create().load()
                : source.load();
        WayangPlatformReadinessProfileValidationReport validation =
                WayangPlatformReadinessProfileValidation.validate(result.profiles(), policy);
        return new SourceEvaluation(result, validation, fallback);
    }

    private static WayangPlatformReadinessProfileSourceStatus status(
            SourceEvaluation evaluation,
            boolean selected) {
        WayangPlatformReadinessProfileSourceResult result = evaluation.result();
        WayangPlatformReadinessProfileValidationReport validation = evaluation.validation();
        return new WayangPlatformReadinessProfileSourceStatus(
                result.sourceId(),
                result.sourceType(),
                result.location(),
                selected,
                evaluation.fallback(),
                result.available(),
                result.available() && validation.valid(),
                result.profileCount(),
                validation.issueCount(),
                result.message());
    }

    private static WayangPlatformReadinessProfileRegistryResolution resolution(
            SourceEvaluation active,
            boolean fallbackUsed,
            List<WayangPlatformReadinessProfileSourceStatus> statuses) {
        WayangPlatformReadinessProfileSourceResult result = active.result();
        return new WayangPlatformReadinessProfileRegistryResolution(
                result.sourceId(),
                result.sourceType(),
                result.location(),
                fallbackUsed,
                statuses,
                result.profiles(),
                active.validation());
    }

    private record SourceEvaluation(
            WayangPlatformReadinessProfileSourceResult result,
            WayangPlatformReadinessProfileValidationReport validation,
            boolean fallback) {
    }
}
