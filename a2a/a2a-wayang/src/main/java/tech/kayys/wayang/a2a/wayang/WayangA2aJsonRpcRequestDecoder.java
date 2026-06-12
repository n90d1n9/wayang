package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Optional;

/**
 * Decodes raw JSON-RPC HTTP bodies into request envelopes.
 */
final class WayangA2aJsonRpcRequestDecoder {

    private WayangA2aJsonRpcRequestDecoder() {
    }

    static WayangA2aJsonRpcRequestDecoder create() {
        return new WayangA2aJsonRpcRequestDecoder();
    }

    Result decode(String body) {
        Map<String, Object> payload;
        try {
            payload = WayangA2aHttpJson.read(body);
        } catch (IllegalArgumentException e) {
            return Result.error(null, WayangA2aJsonRpcError.parseError(e.getMessage()));
        }
        try {
            return Result.request(WayangA2aJsonRpcRequest.fromMap(payload));
        } catch (IllegalArgumentException e) {
            return Result.error(payload.get("id"), WayangA2aJsonRpcError.invalidRequest(e.getMessage()));
        }
    }

    record Result(
            Optional<WayangA2aJsonRpcRequest> request,
            Optional<WayangA2aHttpResponse> error) {

        Result {
            request = request == null ? Optional.empty() : request;
            error = error == null ? Optional.empty() : error;
        }

        static Result request(WayangA2aJsonRpcRequest request) {
            return new Result(Optional.of(request), Optional.empty());
        }

        static Result error(Object id, WayangA2aJsonRpcError error) {
            return new Result(
                    Optional.empty(),
                    Optional.of(WayangA2aJsonRpcHttpResponses.jsonRpc(
                            WayangA2aJsonRpcResponse.error(id, error))));
        }
    }
}
