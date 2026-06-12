package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageResponse;

import java.util.Map;

/**
 * HTTP operation handlers for A2A SendMessage and SendStreamingMessage.
 */
public final class WayangA2aSendMessageHttpHandlers {

    private final WayangA2aSendMessageService service;

    public WayangA2aSendMessageHttpHandlers(WayangA2aSendMessageService service) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        this.service = service;
    }

    public static Map<String, WayangA2aHttpOperationHandler> forService(WayangA2aSendMessageService service) {
        return new WayangA2aSendMessageHttpHandlers(service).handlers();
    }

    public Map<String, WayangA2aHttpOperationHandler> handlers() {
        return WayangA2aHttpHandlerMaps.builder()
                .put(A2aProtocol.OPERATION_SEND_MESSAGE, this::sendMessage)
                .put(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE, this::streamMessage)
                .build();
    }

    private WayangA2aHttpResponse sendMessage(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        WayangA2aSendMessageResult result = service.send(request.sendMessageRequest());
        return WayangA2aHttpResponse.object(200, A2aSendMessageResponse.task(result.responseTask()).toMap());
    }

    private WayangA2aHttpResponse streamMessage(WayangA2aHttpRequest request, WayangA2aHttpRouteMatch match) {
        WayangA2aSendMessageResult result = service.stream(request.sendMessageRequest());
        String event = WayangA2aHttpJson.write(A2aSendMessageResponse.task(result.responseTask()).toMap());
        return WayangA2aHttpResponse.eventStream(200, "data: " + event + "\n\n");
    }
}
