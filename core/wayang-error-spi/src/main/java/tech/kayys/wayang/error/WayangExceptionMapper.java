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
 * JAX-RS exception mapper for Wayang errors.
 */
@Provider
public class WayangExceptionMapper implements ExceptionMapper<WayangException> {

    @Override
    public Response toResponse(WayangException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return Response.status(errorCode.getHttpStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.from(exception))
                .build();
    }
}
