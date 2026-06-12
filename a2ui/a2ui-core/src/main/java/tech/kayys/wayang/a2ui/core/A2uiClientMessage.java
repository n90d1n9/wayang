package tech.kayys.wayang.a2ui.core;

import java.util.Map;

/**
 * One A2UI client-to-server event envelope.
 */
public sealed interface A2uiClientMessage permits A2uiUserAction, A2uiClientError {

    Map<String, Object> toPayload();
}
