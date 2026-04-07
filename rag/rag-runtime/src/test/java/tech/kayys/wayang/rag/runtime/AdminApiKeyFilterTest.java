package tech.kayys.wayang.rag.runtime;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApiKeyFilterTest {

    @Mock
    private ContainerRequestContext requestContext;

    private AdminApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminApiKeyFilter();
    }

    @Test
    void shouldRejectWhenAdminKeyNotConfigured() throws Exception {
        filter.configuredAdminKey = "";
        filter.configuredAdminKeySecondary = "";

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(403, responseCaptor.getValue().getStatus());
    }

    @Test
    void shouldRejectWhenHeaderIsInvalid() throws Exception {
        filter.configuredAdminKey = "secret";
        filter.configuredAdminKeySecondary = "fallback";
        when(requestContext.getHeaderString(AdminApiKeyFilter.ADMIN_KEY_HEADER)).thenReturn("wrong");

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        assertEquals(401, responseCaptor.getValue().getStatus());
    }

    @Test
    void shouldAllowWhenHeaderMatches() throws Exception {
        filter.configuredAdminKey = "secret";
        filter.configuredAdminKeySecondary = "";
        when(requestContext.getHeaderString(AdminApiKeyFilter.ADMIN_KEY_HEADER)).thenReturn("secret");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
        verify(requestContext).setProperty(
                AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY,
                AdminApiKeyFilter.SLOT_PRIMARY);
    }

    @Test
    void shouldAllowWhenHeaderMatchesSecondaryKey() throws Exception {
        filter.configuredAdminKey = "secret";
        filter.configuredAdminKeySecondary = "fallback";
        when(requestContext.getHeaderString(AdminApiKeyFilter.ADMIN_KEY_HEADER)).thenReturn("fallback");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
        verify(requestContext).setProperty(
                AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY,
                AdminApiKeyFilter.SLOT_SECONDARY);
    }
}
