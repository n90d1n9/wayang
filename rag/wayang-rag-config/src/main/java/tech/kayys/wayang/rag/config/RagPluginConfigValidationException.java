package tech.kayys.wayang.rag.config;

import jakarta.ws.rs.BadRequestException;

public class RagPluginConfigValidationException extends BadRequestException {

    private final String code;
    private final String field;
    private final String tenantId;
    private final String value;

    public RagPluginConfigValidationException(
            String code,
            String field,
            String tenantId,
            String value,
            String detail) {
        super("Invalid plugin config [" + code + "] field=`" + field + "` tenant=`"
                + (tenantId == null ? "" : tenantId) + "` value=`"
                + (value == null ? "" : value) + "`: " + detail);
        this.code = code;
        this.field = field;
        this.tenantId = tenantId;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getValue() {
        return value;
    }
}
