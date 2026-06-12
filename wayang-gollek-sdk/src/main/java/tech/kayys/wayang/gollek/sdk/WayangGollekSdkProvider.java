package tech.kayys.wayang.gollek.sdk;

public interface WayangGollekSdkProvider {

    enum Mode {
        LOCAL,
        REMOTE
    }

    Mode mode();

    WayangGollekSdk create(WayangGollekSdkConfig config);

    default int priority() {
        return 100;
    }
}
