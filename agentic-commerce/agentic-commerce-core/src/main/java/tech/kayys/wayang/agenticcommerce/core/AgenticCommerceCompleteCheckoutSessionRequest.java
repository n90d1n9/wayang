package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request body for completing an Agentic Commerce checkout session.
 */
public record AgenticCommerceCompleteCheckoutSessionRequest(
        AgenticCommerceBuyer buyer,
        AgenticCommercePaymentData paymentData,
        Map<String, Object> authenticationResult,
        Map<String, Object> affiliateAttribution,
        Map<String, Object> riskSignals,
        Map<String, Object> metadata) implements AgenticCommerceCheckoutPayload {

    public AgenticCommerceCompleteCheckoutSessionRequest {
        buyer = buyer == null ? AgenticCommerceBuyer.empty() : buyer;
        paymentData = paymentData == null ? AgenticCommercePaymentData.empty() : paymentData;
        authenticationResult = AgenticCommerceMaps.copy(authenticationResult);
        affiliateAttribution = AgenticCommerceMaps.copy(affiliateAttribution);
        riskSignals = AgenticCommerceMaps.copy(riskSignals);
        metadata = AgenticCommerceMaps.copy(metadata);
    }

    public static AgenticCommerceCompleteCheckoutSessionRequest fromMap(Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return new AgenticCommerceCompleteCheckoutSessionRequest(
                    AgenticCommerceBuyer.empty(),
                    AgenticCommercePaymentData.empty(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    Map.of());
        }
        return new AgenticCommerceCompleteCheckoutSessionRequest(
                AgenticCommerceBuyer.fromMap(AgenticCommerceValues.map(values, "buyer")),
                AgenticCommercePaymentData.fromMap(AgenticCommerceValues.map(values, "payment_data", "paymentData")),
                AgenticCommerceValues.map(values, "authentication_result", "authenticationResult"),
                AgenticCommerceValues.map(values, "affiliate_attribution", "affiliateAttribution"),
                AgenticCommerceValues.map(values, "risk_signals", "riskSignals"),
                AgenticCommerceValues.metadata(
                        values,
                        "buyer",
                        "payment_data",
                        "paymentData",
                        "authentication_result",
                        "authenticationResult",
                        "affiliate_attribution",
                        "affiliateAttribution",
                        "risk_signals",
                        "riskSignals"));
    }

    @Override
    public boolean isEmpty() {
        return buyer.isEmpty()
                && paymentData.isEmpty()
                && authenticationResult.isEmpty()
                && affiliateAttribution.isEmpty()
                && riskSignals.isEmpty()
                && metadata.isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        if (!buyer.isEmpty()) {
            values.put("buyer", buyer.toMap());
        }
        if (!paymentData.isEmpty()) {
            values.put("payment_data", paymentData.toMap());
        }
        AgenticCommerceValues.putMap(values, "authentication_result", authenticationResult);
        AgenticCommerceValues.putMap(values, "affiliate_attribution", affiliateAttribution);
        AgenticCommerceValues.putMap(values, "risk_signals", riskSignals);
        AgenticCommerceValues.putMap(values, "metadata", metadata);
        return AgenticCommerceMaps.copy(values);
    }
}
