package tech.kayys.wayang.gollek.sdk.remote;

public final class RemoteWayangGollekException extends RuntimeException {

    public RemoteWayangGollekException(String message) {
        super(message);
    }

    public RemoteWayangGollekException(String message, Throwable cause) {
        super(message, cause);
    }
}
