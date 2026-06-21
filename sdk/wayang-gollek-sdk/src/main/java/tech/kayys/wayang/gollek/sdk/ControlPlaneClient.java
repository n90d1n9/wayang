package tech.kayys.wayang.gollek.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.channels.SocketChannel;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Lightweight control-plane client for the YAFF SHM lifecycle. Parses JSON AllocateResponse.
 * Note: This is a temporary client until the protobuf-based codegen is integrated.
 */
public class ControlPlaneClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    final HttpClient client;
    final URI baseUri;

    public ControlPlaneClient(String baseUrl) {
        this.baseUri = URI.create(baseUrl);
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public AllocateResponse allocate(byte[] payload) throws Exception {
        if ("unix".equalsIgnoreCase(baseUri.getScheme())) {
            // connect to unix domain socket and send op 'A'
            Path socket = Path.of(baseUri.getPath());
            try (SocketChannel sc = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                sc.connect(UnixDomainSocketAddress.of(socket));
                // send op
                ByteBuffer op = ByteBuffer.allocate(1 + Long.BYTES);
                op.put((byte) 'A');
                op.putLong(payload.length);
                op.flip();
                sc.write(op);
                sc.write(ByteBuffer.wrap(payload));
                // read 4-byte len
                ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
                while (lenBuf.hasRemaining()) if (sc.read(lenBuf) < 0) throw new IOException("early EOF");
                lenBuf.flip();
                int len = lenBuf.getInt();
                byte[] resp = new byte[len];
                ByteBuffer respBuf = ByteBuffer.wrap(resp);
                while (respBuf.hasRemaining()) if (sc.read(respBuf) < 0) throw new IOException("early EOF");
                return MAPPER.readValue(resp, AllocateResponse.class);
            }
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/allocate"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Control-plane returned status " + resp.statusCode());
        }
        AllocateResponse ar = MAPPER.readValue(resp.body(), AllocateResponse.class);
        return ar;
    }

    public ReleaseResponse release(String id) throws Exception {
        byte[] body = MAPPER.writeValueAsBytes(java.util.Map.of("id", id));
        if ("unix".equalsIgnoreCase(baseUri.getScheme())) {
            Path socket = Path.of(baseUri.getPath());
            try (SocketChannel sc = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                sc.connect(UnixDomainSocketAddress.of(socket));
                ByteBuffer op = ByteBuffer.allocate(1 + Long.BYTES);
                op.put((byte) 'R');
                op.putLong(body.length);
                op.flip();
                sc.write(op);
                sc.write(ByteBuffer.wrap(body));
                ByteBuffer lenBuf = ByteBuffer.allocate(Integer.BYTES);
                while (lenBuf.hasRemaining()) if (sc.read(lenBuf) < 0) throw new IOException("early EOF");
                lenBuf.flip();
                int len = lenBuf.getInt();
                byte[] resp = new byte[len];
                ByteBuffer respBuf = ByteBuffer.wrap(resp);
                while (respBuf.hasRemaining()) if (sc.read(respBuf) < 0) throw new IOException("early EOF");
                return MAPPER.readValue(resp, ReleaseResponse.class);
            }
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(baseUri.resolve("/release"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("Control-plane returned status " + resp.statusCode());
        }
        ReleaseResponse rr = MAPPER.readValue(resp.body(), ReleaseResponse.class);
        return rr;
    }

    public static class AllocateResponse {
        public String id;
        public String path;
        public long offset;
        public long length;
        public byte[] metadata;

        public AllocateResponse() {}
    }

    public static class ReleaseResponse {
        public boolean ok;
        public String message;
        public ReleaseResponse() {}
    }
}
