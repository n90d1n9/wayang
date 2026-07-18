package tech.kayys.wayang.yaff;

import java.nio.ByteBuffer;
import java.lang.reflect.Method;

/**
 * Reflection-based adapter that wraps a class from an external module that follows a simple
 * YAFF transport shape (marshal/unmarshal/send). This avoids a hard compile-time dependency
 * on the external adapter implementation when the adapter is maintained in a different module.
 *
 * The reflection wrapper accepts delegates that implement either ByteBuffer or MemorySegment
 * based sendRequest signatures. MemorySegment support is attempted via reflection only if
 * the runtime provides the FFM API.
 */
public class ReflectionWayangYaffTransportProvider implements WayangYaffTransportProvider {

    private final Object delegate;
    private final Method sendMethod;
    private final boolean acceptsByteBuffer;
    private final int priority;
    private final String id;

    public ReflectionWayangYaffTransportProvider(String delegateClassName) {
        try {
            Class<?> cls = Class.forName(delegateClassName);
            this.delegate = cls.getDeclaredConstructor().newInstance();
            // Try to find a sendRequest(ByteBuffer) method first
            Method found = null;
            boolean bb = false;
            for (Method mm : cls.getMethods()) {
                if (mm.getName().equals("sendRequest") && mm.getParameterCount() == 1) {
                    Class<?> p = mm.getParameterTypes()[0];
                    if (p == ByteBuffer.class) {
                        found = mm;
                        bb = true;
                        break;
                    }
                }
            }
            // if not found, try to locate a MemorySegment-based method via name (best-effort)
            if (found == null) {
                for (Method mm : cls.getMethods()) {
                    if (mm.getName().equals("sendRequest") && mm.getParameterCount() == 1) {
                        found = mm;
                        bb = false;
                        break;
                    }
                }
            }
            if (found == null) throw new IllegalArgumentException("Delegate class has no sendRequest method");
            this.sendMethod = found;
            this.acceptsByteBuffer = bb;
            this.priority = 50;
            this.id = delegateClassName;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate reflection YAFF transport delegate: " + delegateClassName, t);
        }
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public ByteBuffer sendRequest(ByteBuffer request) throws Exception {
        if (acceptsByteBuffer) {
            Object result = sendMethod.invoke(delegate, request);
            if (result == null) return null;
            if (result instanceof ByteBuffer) return (ByteBuffer) result;
            throw new IllegalStateException("Delegate returned unexpected type: " + result.getClass());
        }
        // Delegate expects another type (e.g., MemorySegment). Try to call it and attempt conversion.
        Object result = sendMethod.invoke(delegate, request);
        if (result == null) return null;
        if (result instanceof ByteBuffer) return (ByteBuffer) result;
        // If result is not ByteBuffer (e.g., MemorySegment), attempt best-effort conversion is not implemented.
        throw new IllegalStateException("Delegate returned unsupported type: " + result.getClass());
    }
}
