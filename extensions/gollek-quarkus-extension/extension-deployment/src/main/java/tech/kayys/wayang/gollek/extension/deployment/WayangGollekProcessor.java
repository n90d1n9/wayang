package tech.kayys.wayang.gollek.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;

public class WayangGollekProcessor {

    @BuildStep
    ExtensionSslNativeSupportBuildItem setup() {
        // simple placeholder build step
        return new ExtensionSslNativeSupportBuildItem(false);
    }
}
