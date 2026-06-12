package tech.kayys.wayang.a2a.core;

/**
 * Stable A2A protocol constants used by neutral contracts and adapters.
 */
public final class A2aProtocol {

    public static final String VERSION = "1.0";

    public static final String MEDIA_TYPE = "application/a2a+json";
    public static final String EVENT_STREAM_MEDIA_TYPE = "text/event-stream";
    public static final String WELL_KNOWN_AGENT_CARD_PATH = "/.well-known/agent-card.json";

    public static final String BINDING_JSONRPC = "JSONRPC";
    public static final String BINDING_GRPC = "GRPC";
    public static final String BINDING_HTTP_JSON = "HTTP+JSON";

    public static final String OPERATION_DISCOVER_AGENT_CARD = "DiscoverAgentCard";
    public static final String OPERATION_SEND_MESSAGE = "SendMessage";
    public static final String OPERATION_SEND_STREAMING_MESSAGE = "SendStreamingMessage";
    public static final String OPERATION_GET_TASK = "GetTask";
    public static final String OPERATION_LIST_TASKS = "ListTasks";
    public static final String OPERATION_CANCEL_TASK = "CancelTask";
    public static final String OPERATION_SUBSCRIBE_TO_TASK = "SubscribeToTask";
    public static final String OPERATION_CREATE_TASK_PUSH_NOTIFICATION_CONFIG =
            "CreateTaskPushNotificationConfig";
    public static final String OPERATION_GET_TASK_PUSH_NOTIFICATION_CONFIG =
            "GetTaskPushNotificationConfig";
    public static final String OPERATION_LIST_TASK_PUSH_NOTIFICATION_CONFIGS =
            "ListTaskPushNotificationConfigs";
    public static final String OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG =
            "DeleteTaskPushNotificationConfig";
    public static final String OPERATION_GET_EXTENDED_AGENT_CARD = "GetExtendedAgentCard";

    public static final String HEADER_EXTENSIONS = "A2A-Extensions";
    public static final String HEADER_VERSION = "A2A-Version";

    private A2aProtocol() {
    }
}
