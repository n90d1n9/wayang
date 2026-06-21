package tech.kayys.wayang.gollek.sdk;

import java.nio.ByteBuffer;

/**
 * SPI for YAFF transports. Implementations may use shared-memory transports to exchange
 * YAFF-encoded ByteBuffers between Wayang and local inference runtimes. Using ByteBuffer
 * avoids reliance on preview FFM APIs during prototyping; later migration to MemorySegment
 * is possible when FFM is ready.
 */
public interface WayangYaffTransportProvider {

    /** Lower value = higher priority */
    default int priority() { return 100; }

    /** Short identifier for the transport provider. */
    String id();

    /**
     * Send a YAFF-encoded ByteBuffer to the inference runtime and return a response buffer.
     * Implementations decide ownership semantics for the provided and returned buffers.
     */
    ByteBuffer sendRequest(ByteBuffer request) throws Exception;

}
