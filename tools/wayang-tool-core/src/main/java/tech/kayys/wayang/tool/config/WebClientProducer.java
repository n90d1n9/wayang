package tech.kayys.wayang.tool.config;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

@ApplicationScoped
public class WebClientProducer {

    @Produces
    @Singleton
    @Alternative
    @Priority(1)
    public WebClient mutinyWebClient(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions()
                .setFollowRedirects(true)
                .setConnectTimeout(5000)
                .setIdleTimeout(60));
    }

    @Produces
    @Singleton
    public io.vertx.ext.web.client.WebClient coreWebClient(io.vertx.core.Vertx vertx) {
        return io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                .setFollowRedirects(true)
                .setConnectTimeout(5000)
                .setIdleTimeout(60));
    }
}
