package tech.kayys.wayang.client;

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
