package tech.kayys.wayang.hitl.runtime.config;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class TestWebClientProducer {

    @Produces
    @ApplicationScoped
    public WebClient mutinyWebClient(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout(2000)
                .setIdleTimeout(10));
    }

    @Produces
    @ApplicationScoped
    public io.vertx.ext.web.client.WebClient coreWebClient(io.vertx.core.Vertx vertx) {
        return io.vertx.ext.web.client.WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout(2000)
                .setIdleTimeout(10));
    }
}
