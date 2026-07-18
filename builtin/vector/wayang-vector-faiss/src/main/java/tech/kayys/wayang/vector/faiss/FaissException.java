package tech.kayys.wayang.vector.faiss;

/**
 * Exception thrown when a FAISS native operation fails.
 */
public class FaissException extends RuntimeException {

    public FaissException(String message) {
        super(message);
    }

    public FaissException(String message, Throwable cause) {
        super(message, cause);
    }
}
