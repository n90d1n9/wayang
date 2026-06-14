package tech.kayys.wayang.gollek.sdk;

@FunctionalInterface
public interface WayangPlatformReadinessAssessor {

    WayangReadinessReport assess(WayangGollekSdk sdk);
}
