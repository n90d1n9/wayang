package tech.kayys.wayang.gollek.sdk;

import java.nio.ByteBuffer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Transport provider that forwards raw YAFF bytes to a control-plane HTTP service.
 * The control-plane is expected to implement a simple POST /allocate endpoint that
 * accepts raw bytes and returns a JSON metadata response. For now the provider
 * returns that JSON payload as the response ByteBuffer so callers can inspect
 * allocation metadata. Later this can be enriched to orchestrate a full RPC flow.
 */
public class ControlPlaneWayangYaffTransportProvider implements WayangYaffTransportProvider {

    private final ControlPlaneClient client;
    private final int priority;

    public ControlPlaneWayangYaffTransportProvider(String baseUrl, int priority) {
        this.client = new ControlPlaneClient(baseUrl);
        this.priority = priority;
    }

    public ControlPlaneWayangYaffTransportProvider(String baseUrl) {
        this(baseUrl, 10);
    }

    @Override
    public int priority() { return priority; }

    @Override
    public String id() { return "control-plane:" + client.baseUri.getHost() + ":" + client.baseUri.getPort(); }

    @Override
    public ByteBuffer sendRequest(ByteBuffer request) throws Exception {
        byte[] bytes = new byte[request.remaining()];
        request.get(bytes);
        ControlPlaneClient.AllocateResponse ar = client.allocate(bytes);
        // Return the AllocateResponse serialized as JSON bytes so callers can inspect metadata.
        byte[] out = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsBytes(ar);
        return ByteBuffer.wrap(out);
    }
}
