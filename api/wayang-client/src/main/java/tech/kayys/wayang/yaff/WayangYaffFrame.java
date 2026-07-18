package tech.kayys.wayang.yaff;

import tech.kayys.wayang.client.ControlPlaneClient;
import tech.kayys.wayang.client.ControlPlaneClient.AllocateResponse;
import tech.kayys.wayang.client.ControlPlaneClient.ReleaseResponse;

/**
 * Simple frame handle representing an allocated YAFF SHM frame from the control-plane.
 */
public final class WayangYaffFrame implements AutoCloseable {
    private final String controlUrl;
    private final String id;
    private final String path;
    private final long offset;
    private final long length;

    public WayangYaffFrame(String controlUrl, String id, String path, long offset, long length) {
        this.controlUrl = controlUrl;
        this.id = id;
        this.path = path;
        this.offset = offset;
        this.length = length;
    }

    public String id() { return id; }
    public String path() { return path; }
    public long offset() { return offset; }
    public long length() { return length; }

    public static WayangYaffFrame allocate(String controlUrl, byte[] payload) throws Exception {
        ControlPlaneClient client = new ControlPlaneClient(controlUrl);
        ControlPlaneClient.AllocateResponse ar = client.allocate(payload);
        return new WayangYaffFrame(controlUrl, ar.id, ar.path, ar.offset, ar.length);
    }

    public boolean release() throws Exception {
        if (controlUrl == null || controlUrl.isBlank()) return false;
        ControlPlaneClient client = new ControlPlaneClient(controlUrl);
        ControlPlaneClient.ReleaseResponse rr = client.release(id);
        return rr != null && rr.ok;
    }

    @Override
    public void close() {
        try { release(); } catch (Exception ignored) {}
    }
}
