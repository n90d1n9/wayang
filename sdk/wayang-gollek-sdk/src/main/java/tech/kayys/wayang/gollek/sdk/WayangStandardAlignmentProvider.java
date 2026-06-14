package tech.kayys.wayang.gollek.sdk;

/**
 * Classpath extension point for protocol modules that can report standard alignment.
 */
public interface WayangStandardAlignmentProvider {

    String providerId();

    WayangStandardAlignmentPortfolio portfolio();

    default int priority() {
        return 100;
    }
}
