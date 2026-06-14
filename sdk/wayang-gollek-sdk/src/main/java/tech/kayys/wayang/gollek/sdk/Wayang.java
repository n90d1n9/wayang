package tech.kayys.wayang.gollek.sdk;

public final class Wayang {

    private Wayang() {
    }

    public static WayangClient client() {
        return WayangClient.local();
    }

    public static WayangClient client(WayangGollekSdkConfig config) {
        return WayangClient.create(config);
    }

    public static WayangClient client(WayangGollekSdk sdk) {
        return WayangClient.of(sdk);
    }

    public static WayangGollekSdk local() {
        return WayangGollekSdkFactory.createLocalSdk();
    }

    public static WayangGollekSdk remote(String endpoint, String apiKey) {
        return WayangGollekSdkFactory.createRemoteSdk(endpoint, apiKey);
    }

    public static WayangGollekSdk create(WayangGollekSdkConfig config) {
        return WayangGollekSdkFactory.create(config);
    }
}
