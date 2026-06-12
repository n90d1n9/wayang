package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMetricExchange;

/**
 * Minimal metric-facing view shared by A2UI HTTP exchange records.
 */
public interface HttpMetricExchange extends TransportMetricExchange {

    int statusCode();

    boolean successful();
}
