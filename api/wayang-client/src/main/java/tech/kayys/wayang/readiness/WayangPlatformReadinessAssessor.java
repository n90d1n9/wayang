package tech.kayys.wayang.readiness;

import tech.kayys.wayang.client.WayangGollekSdk;

@FunctionalInterface
public interface WayangPlatformReadinessAssessor {

   public  WayangReadinessReport assess(WayangGollekSdk sdk);
}
