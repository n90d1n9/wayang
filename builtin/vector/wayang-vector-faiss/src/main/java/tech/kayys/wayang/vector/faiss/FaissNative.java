package tech.kayys.wayang.vector.faiss;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Native library loader and error handling for FAISS FFM bindings.
 * <p>
 * Loads {@code libfaiss_c} from {@code ~/.wayang/lib/} via JDK 25
 * Foreign Function &amp; Memory (FFM) API.
 */
public final class FaissNative {

    private static final SymbolLookup FAISS;
    private static final MethodHandle GET_LAST_ERROR;

    static {
        // Resolve library path
        Path libDir = Path.of(System.getProperty("user.home"), ".wayang", "lib");
        String libPath = System.getProperty("wayang.faiss.lib.path",
                libDir.toAbsolutePath().toString());

        SymbolLookup lookup;
        try {
            // Try platform-specific library name
            String libName = System.getProperty("os.name").toLowerCase().contains("mac")
                    ? "libfaiss_c.dylib" : "libfaiss_c.so";
            Path fullPath = Path.of(libPath, libName);
            if (Files.exists(fullPath)) {
                lookup = SymbolLookup.libraryLookup(fullPath, Arena.global());
            } else {
                // Fallback: try system library path
                lookup = SymbolLookup.libraryLookup("faiss_c", Arena.global());
            }
        } catch (Exception e) {
            throw new UnsatisfiedLinkError(
                    "Failed to load libfaiss_c from " + libPath +
                    ". Run scripts/build-faiss.sh first. Error: " + e.getMessage());
        }

        FAISS = lookup;

        // faiss_get_last_error() -> const char*
        GET_LAST_ERROR = downcallHandle("faiss_get_last_error",
                FunctionDescriptor.of(ValueLayout.ADDRESS));
    }

    private FaissNative() {}

    /**
     * Get the FAISS symbol lookup for resolving function addresses.
     */
    public static SymbolLookup lookup() {
        return FAISS;
    }

    /**
     * Create a downcall method handle for a FAISS C function.
     *
     * @param name       the C function name
     * @param descriptor the function descriptor (return type + param types)
     * @return the bound MethodHandle
     */
    public static MethodHandle downcallHandle(String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = FAISS.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError(
                        "FAISS symbol not found: " + name +
                        ". Ensure libfaiss_c is built with all C API modules."));
        return Linker.nativeLinker().downcallHandle(symbol, descriptor);
    }

    /**
     * Find a FAISS symbol, returning empty if not available.
     */
    public static Optional<MemorySegment> findSymbol(String name) {
        return FAISS.find(name);
    }

    /**
     * Check the return code from a FAISS C function.
     * FAISS C API returns 0 on success, non-zero on error.
     *
     * @param rc return code from a FAISS C function
     * @throws FaissException if rc != 0
     */
    public static void checkError(int rc) {
        if (rc != 0) {
            String message = getLastError();
            throw new FaissException("FAISS error (rc=" + rc + "): " +
                    (message != null ? message : "unknown error"));
        }
    }

    /**
     * Get the last FAISS error message.
     */
    public static String getLastError() {
        try {
            MemorySegment ptr = (MemorySegment) GET_LAST_ERROR.invokeExact();
            if (ptr.equals(MemorySegment.NULL)) {
                return null;
            }
            return ptr.reinterpret(Long.MAX_VALUE).getString(0);
        } catch (Throwable t) {
            return "Failed to retrieve error: " + t.getMessage();
        }
    }

    // ==================== Metric Type Constants ====================

    /** Maximum inner product search */
    public static final int METRIC_INNER_PRODUCT = 0;
    /** Squared L2 search */
    public static final int METRIC_L2 = 1;
    /** L1 (cityblock) distance */
    public static final int METRIC_L1 = 2;
    /** Infinity distance */
    public static final int METRIC_Linf = 3;
    /** Lp distance (p given by metric_arg) */
    public static final int METRIC_Lp = 4;
    /** Canberra distance */
    public static final int METRIC_Canberra = 20;
    /** Bray-Curtis distance */
    public static final int METRIC_BrayCurtis = 21;
    /** Jensen-Shannon divergence */
    public static final int METRIC_JensenShannon = 22;

    // ==================== I/O Flag Constants ====================

    /** Memory-map the index */
    public static final int IO_FLAG_MMAP = 1;
    /** Read-only mode */
    public static final int IO_FLAG_READ_ONLY = 2;
}
