package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.factory.GollekSdkFactory;
import tech.kayys.gollek.sdk.config.SdkConfig;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;

import java.time.Duration;

/**
 * CDI producer that creates and manages the {@link GollekSdk} singleton.
 *
 * <p>This is an {@link Alternative} producer with low priority, making it a
 * <b>passive fallback</b> when gollek's own CDI beans are not available.
 * The gollek project provides {@code LocalGollekSdk} as the primary bean.
 *
 * <p>Selection logic (mirrors the Gollek SDK factory):
 * <ol>
 *   <li>If {@code gamelan.engine.mode=local} — use the local in-process engine</li>
 *   <li>If {@code gamelan.engine.mode=remote} and {@code gamelan.remote.url} is set — use HTTP</li>
 *   <li>Otherwise — auto-detect (local preferred over remote)</li>
 * </ol>
 */
@ApplicationScoped
@Alternative
@Priority(100)
public class SdkProvider {

    private static final Logger log = LoggerFactory.getLogger(SdkProvider.class);

    @ConfigProperty(name = "gamelan.engine.mode", defaultValue = "auto")
    String engineMode;

    @ConfigProperty(name = "gamelan.remote.url", defaultValue = "")
    String remoteUrl;

    @ConfigProperty(name = "gamelan.api.key", defaultValue = "community")
    String apiKey;

    @ConfigProperty(name = "gamelan.request.timeout.seconds", defaultValue = "120")
    int requestTimeoutSeconds;

    private GollekSdk cachedSdk;

    /**
     * Produces the {@link GollekSdk} bean.
     */
    @Produces
    @ApplicationScoped
    public GollekSdk produceSdk() {
        if (cachedSdk != null) return cachedSdk;
        cachedSdk = createSdk();
        log.info("Gollek SDK initialized [mode={}]", engineMode);
        return cachedSdk;
    }

    /** Also exposed directly for non-CDI use. */
    public GollekSdk sdk() {
        if (cachedSdk == null) cachedSdk = createSdk();
        return cachedSdk;
    }

    private GollekSdk createSdk() {
        SdkConfig sdkConfig = SdkConfig.builder()
                .apiKey(apiKey)
                .requestTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                .build();

        try {
            return switch (engineMode.toLowerCase()) {
                case "local" -> {
                    log.info("Creating local Gollek SDK");
                    yield GollekSdkFactory.createLocalSdk(sdkConfig);
                }
                case "remote" -> {
                    if (remoteUrl.isBlank()) {
                        throw new IllegalStateException(
                                "gamelan.remote.url must be set when engine.mode=remote");
                    }
                    log.info("Creating remote Gollek SDK -> {}", remoteUrl);
                    yield GollekSdkFactory.createRemoteSdk(
                            SdkConfig.builder()
                                    .apiKey(apiKey)
                                    .baseUrl(remoteUrl)
                                    .requestTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                                    .build());
                }
                default -> {
                    log.info("Auto-detecting Gollek SDK mode");
                    yield GollekSdkFactory.create(sdkConfig);
                }
            };
        } catch (SdkException e) {
            throw new RuntimeException("Failed to initialize Gollek SDK: " + e.getMessage(), e);
        }
    }
}
