package tech.kayys.wayang.rag.runtime;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApiKeyResponseFilterTest {

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    private AdminApiKeyResponseFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminApiKeyResponseFilter();
    }

    @Test
    void shouldAddSlotHeaderWhenSlotExists() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(requestContext.getProperty(AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY)).thenReturn("secondary");
        when(responseContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext, responseContext);

        assertEquals("secondary", headers.getFirst(AdminApiKeyFilter.ADMIN_KEY_SLOT_HEADER));
    }

    @Test
    void shouldSkipHeaderWhenSlotMissing() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(requestContext.getProperty(AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY)).thenReturn(null);

        filter.filter(requestContext, responseContext);

        assertNull(headers.getFirst(AdminApiKeyFilter.ADMIN_KEY_SLOT_HEADER));
    }
}
