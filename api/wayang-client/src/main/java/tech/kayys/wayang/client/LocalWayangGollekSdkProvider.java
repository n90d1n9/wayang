package tech.kayys.wayang.client;

public final class LocalWayangGollekSdkProvider implements WayangGollekSdkProvider {

    @Override
    public Mode mode() {
        return Mode.LOCAL;
    }

    @Override
    public WayangGollekSdk create(WayangGollekSdkConfig config) {
        return new LocalWayangGollekSdk(config);
    }

    @Override
    public int priority() {
        return 10;
    }
}
