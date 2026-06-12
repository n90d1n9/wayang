package tech.kayys.wayang.a2a.wayang;

/**
 * Shared Wayang/A2A metadata keys.
 */
public final class WayangA2a {

    public static final String CONTEXT_KEY = "a2a";
    public static final String MESSAGE_ID_KEY = "messageId";
    public static final String CONTEXT_ID_KEY = "contextId";
    public static final String TASK_ID_KEY = "taskId";
    public static final String EXTENSIONS_KEY = "extensions";
    public static final String PARTS_KEY = "parts";
    public static final String METADATA_KEY = "metadata";
    public static final String CONFIGURATION_KEY = "configuration";
    public static final String TENANT_KEY = "tenant";
    public static final String SEND_MESSAGE_REQUEST_ATTRIBUTE = "sendMessageRequest";

    public static final String METADATA_ALLOWED_SKILLS = "allowedSkills";
    public static final String METADATA_INPUT_MODES = "inputModes";
    public static final String METADATA_OUTPUT_MODES = "outputModes";
    public static final String METADATA_EXAMPLES = "examples";
    public static final String METADATA_SECURITY_REQUIREMENTS = "securityRequirements";

    public static final String DEFAULT_TEXT_MEDIA_TYPE = "text/plain";
    public static final String DEFAULT_JSON_MEDIA_TYPE = "application/json";
    public static final String DEFAULT_AGENT_VERSION = "1.0.0";

    private WayangA2a() {
    }
}
