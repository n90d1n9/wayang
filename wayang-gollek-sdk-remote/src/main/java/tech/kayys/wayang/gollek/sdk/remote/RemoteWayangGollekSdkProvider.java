package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkProvider;

public final class RemoteWayangGollekSdkProvider implements WayangGollekSdkProvider {

    @Override
    public Mode mode() {
        return Mode.REMOTE;
    }

    @Override
    public WayangGollekSdk create(WayangGollekSdkConfig config) {
        return new RemoteWayangGollekSdk(config);
    }

    @Override
    public int priority() {
        return 50;
    }
}
