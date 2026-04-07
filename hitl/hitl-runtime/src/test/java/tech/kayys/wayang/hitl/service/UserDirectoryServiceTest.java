package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserDirectoryServiceTest {

    private UserDirectoryService userDirectoryService;

    @BeforeEach
    void setUp() {
        userDirectoryService = new UserDirectoryService();
        // Initialize the cache as PostConstruct would do
        userDirectoryService.init();
    }

    @Test
    void shouldGetUserEmailSuccessfully() {
        // Given
        String userId = "user1";

        // When
        Uni<String> result = userDirectoryService.getUserEmail(userId);

        // Then
        String email = result.await().indefinitely();
        assertEquals("[EMAIL_ADDRESS]", email);
    }

    @Test
    void shouldReturnNullForUnknownUser() {
        // Given
        String userId = "unknown-user";

        // When
        Uni<String> result = userDirectoryService.getUserEmail(userId);

        // Then
        String email = result.await().indefinitely();
        assertNull(email);
    }

    @Test
    void shouldGetUserInfoSuccessfully() {
        // Given
        String userId = "user2";

        // When
        Uni<UserDirectoryService.UserInfo> result = userDirectoryService.getUserInfo(userId);

        // Then
        UserDirectoryService.UserInfo userInfo = result.await().indefinitely();
        assertNotNull(userInfo);
        assertEquals("user2", userInfo.userId());
        assertEquals("[EMAIL_ADDRESS]", userInfo.email());
        assertEquals("Jane Smith", userInfo.displayName());
    }

    @Test
    void shouldReturnNullUserInfoForUnknownUser() {
        // Given
        String userId = "unknown-user";

        // When
        Uni<UserDirectoryService.UserInfo> result = userDirectoryService.getUserInfo(userId);

        // Then
        UserDirectoryService.UserInfo userInfo = result.await().indefinitely();
        assertNull(userInfo);
    }

    @Test
    void shouldInitializeWithDemoUsers() {
        // When & Then
        Uni<String> user1Email = userDirectoryService.getUserEmail("user1");
        Uni<String> user2Email = userDirectoryService.getUserEmail("user2");
        Uni<String> adminEmail = userDirectoryService.getUserEmail("admin");

        assertEquals("[EMAIL_ADDRESS]", user1Email.await().indefinitely());
        assertEquals("[EMAIL_ADDRESS]", user2Email.await().indefinitely());
        assertEquals("[EMAIL_ADDRESS]", adminEmail.await().indefinitely());
    }
}
