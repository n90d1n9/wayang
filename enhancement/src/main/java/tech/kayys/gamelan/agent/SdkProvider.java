package tech.kayys.gamelan.agent;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gollek.factory.GollekSdkFactory;
import tech.kayys.gollek.sdk.config.SdkConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;

import java.time.Duration;

/**
 * CDI producer for the {@link GollekSdk} singleton.
 *
 * <h2>Mode selection</h2>
 * <pre>
 * gamelan.engine.mode = auto   → tries local first, falls back to remote
 * gamelan.engine.mode = local  → local in-process Gollek engine
 * gamelan.engine.mode = remote → HTTP client to gamelan.remote.url
 * </pre>
 *
 * <h2>Startup validation</h2>
 * {@link #init()} is called by CDI after injection. It calls {@link GollekSdk#getSystemInfo()}
 * to verify the engine is reachable. If it fails, the error is logged but the
 * application continues — some commands (config, skill list) do not need the engine.
 *
 * <h2>Thread safety</h2>
 * The produced bean is {@code @ApplicationScoped} so CDI ensures it is a
 * singleton. The {@code produceSdk()} method is only called once.
 */
@ApplicationScoped
public class SdkProvider {

    private static final Logger log = LoggerFactory.getLogger(SdkProvider.class);

    @Inject GamelanConfig config;

    private GollekSdk sdk;
    private String    resolvedMode;
    private boolean   healthy;

    @PostConstruct
    void init() {
        try {
            sdk         = build();
            resolvedMode = config.engineMode();
            // Smoke-test: verify reachability
            sdk.getSystemInfo();
            healthy = true;
            log.info("Gollek SDK ready [mode={}, healthy=true]", resolvedMode);
        } catch (Exception e) {
            // Don't crash the whole app — commands that need the SDK will fail with a clear message
            healthy = false;
            log.warn("Gollek SDK initialised but engine may be unreachable: {}", e.getMessage());
        }
    }

    @Produces
    @ApplicationScoped
    public GollekSdk produceSdk() {
        if (sdk == null) {
            // Fallback: try again (useful if init() ran before config was ready)
            try { sdk = build(); } catch (Exception e) {
                throw new IllegalStateException(
                        "Gollek SDK is not available. Check engine mode and connection.\n"
                        + "  Configured mode: " + config.engineMode() + "\n"
                        + "  Remote URL:      " + config.remoteUrl() + "\n"
                        + "  Error:           " + e.getMessage(), e);
            }
        }
        return sdk;
    }

    /** Directly access the SDK (for non-CDI contexts). */
    public GollekSdk sdk()     { return produceSdk(); }
    public boolean   healthy() { return healthy; }
    public String    mode()    { return resolvedMode; }

    // ── Private ────────────────────────────────────────────────────────────

    private GollekSdk build() throws SdkException {
        SdkConfig base = SdkConfig.builder()
                .apiKey(config.apiKey())
                .requestTimeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                .build();

        return switch (config.engineMode().toLowerCase()) {
            case "local" -> {
                log.info("Initialising local Gollek SDK");
                yield GollekSdkFactory.createLocalSdk(base);
            }
            case "remote" -> {
                String url = config.remoteUrl();
                if (url.isBlank()) throw new IllegalStateException(
                        "gamelan.engine.mode=remote requires gamelan.remote.url to be set");
                log.info("Initialising remote Gollek SDK → {}", url);
                yield GollekSdkFactory.createRemoteSdk(
                        SdkConfig.builder()
                                .apiKey(config.apiKey())
                                .baseUrl(url)
                                .requestTimeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
                                .build());
            }
            default -> {
                log.info("Auto-detecting Gollek SDK mode");
                yield GollekSdkFactory.create(base);
            }
        };
    }
}
