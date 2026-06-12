package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.agent.hermes.HermesPortDispatchResult;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Maps Hermes runtime port outcomes to HTTP responses.
 */
final class HermesPortResponseMapper {

    Response missingPort(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ApiErrorResponse(message))
                .build();
    }

    Response dispatch(Supplier<HermesPortDispatchResult> dispatch) {
        return dispatch(dispatch, Function.identity());
    }

    <T> Response dispatch(
            Supplier<HermesPortDispatchResult> dispatch,
            Function<HermesPortResponse, T> responseBody) {
        try {
            HermesPortResponse body = HermesPortResponse.from(dispatch.get());
            Response.Status status = body.successful()
                    ? Response.Status.OK
                    : Response.Status.SERVICE_UNAVAILABLE;
            return Response.status(status)
                    .entity(responseBody.apply(body))
                    .build();
        } catch (IllegalArgumentException error) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiErrorResponse.from(error))
                    .build();
        } catch (RuntimeException error) {
            return Response.serverError()
                    .entity(ApiErrorResponse.from(error))
                    .build();
        }
    }
}
