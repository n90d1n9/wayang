/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Fallback JAX-RS exception mapper for unexpected errors.
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        WayangException wrapped = new WayangException(
                errorCode,
                exception.getMessage() != null ? exception.getMessage() : "Unexpected error",
                exception);
        return Response.status(errorCode.getHttpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.from(wrapped))
                .build();
    }
}
