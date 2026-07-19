package tech.kayys.wayang.inference;

import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.wayang.factory.GollekSdkFactory;

/**
 * Factory for creating WayangInferenceService instances.
 * Falls back to subprocess-based inference if SDK initialization fails.
 */
public class WayangInferenceServiceFactory {
    
    private static GollekSdk cachedSdk;
    private static boolean sdkInitAttempted = false;
    
    /**
     * Create an inference service with the default Gollek SDK configuration.
     * Falls back to subprocess if SDK initialization fails.
     * 
     * @param systemPrompt system prompt for the inference service
     * @param modelId the model to use for inference
     * @return inference service instance (with fallback to subprocess)
     */
    public static WayangInferenceService create(String systemPrompt, String modelId) {
        try {
            GollekSdk sdk = getOrCreateSdk();
            return new WayangInferenceService(sdk, systemPrompt, modelId);
        } catch (Exception e) {
            // SDK initialization failed - return service with subprocess fallback
            return new WayangInferenceService(null, systemPrompt, modelId);
        }
    }
    
    /**
     * Create an inference service with explicit SDK.
     * 
     * @param sdk Gollek SDK instance (or null for subprocess fallback)
     * @param systemPrompt system prompt for the inference service
     * @param modelId the model to use for inference
     * @return inference service instance
     */
    public static WayangInferenceService create(GollekSdk sdk, String systemPrompt, String modelId) {
        return new WayangInferenceService(sdk, systemPrompt, modelId);
    }
    
    /**
     * Get or create the default Gollek SDK instance (cached).
     * 
     * @return Gollek SDK instance
     * @throws SdkException if SDK creation fails
     */
    public static GollekSdk getOrCreateSdk() throws SdkException {
        if (cachedSdk != null) {
            return cachedSdk;
        }
        
        if (sdkInitAttempted) {
            throw new SdkException("SDK_INIT_FAILED", "SDK initialization previously failed");
        }
        
        sdkInitAttempted = true;
        
        try {
            // Ensure embedded/SDK mode runs the GGUF daemon quietly,
            // suppressing verbose headers and performance metrics from reaching the TUI.
            System.setProperty("gollek.gguf.fast_run.quiet", "true");
            
            cachedSdk = GollekSdkFactory.createLocalSdk();
            return cachedSdk;
        } catch (SdkException e) {
            throw e;
        } catch (Exception e) {
            throw new SdkException("SDK_INIT_FAILED", "Unexpected error initializing Gollek SDK: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if Gollek SDK is available.
     * 
     * @return true if SDK can be loaded
     */
    public static boolean isAvailable() {
        try {
            getOrCreateSdk();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Close/cleanup resources.
     */
    public static void shutdown() {
        cachedSdk = null;
        sdkInitAttempted = false;
    }
}
