package tech.kayys.wayang.hitl.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to look up user information
 * In production, integrate with LDAP, Active Directory, or user management
 * service
 */
@ApplicationScoped
public class UserDirectoryService {

    private static final Logger LOG = LoggerFactory.getLogger(UserDirectoryService.class);

    // In-memory cache for demo
    private final Map<String, UserInfo> userCache = new HashMap<>();

    @jakarta.annotation.PostConstruct
    void init() {
        // Populate with demo users
        userCache.put("user1", new UserInfo("user1", "[EMAIL_ADDRESS]", "John Doe"));
        userCache.put("user2", new UserInfo("user2", "[EMAIL_ADDRESS]", "Jane Smith"));
        userCache.put("admin", new UserInfo("admin", "[EMAIL_ADDRESS]", "Admin User"));
    }

    public Uni<String> getUserEmail(String userId) {
        UserInfo user = userCache.get(userId);
        if (user == null) {
            LOG.warn("User not found: {}", userId);
            // In production, query user directory
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(user.email);
    }

    public Uni<UserInfo> getUserInfo(String userId) {
        UserInfo user = userCache.get(userId);
        return user != null ? Uni.createFrom().item(user) : Uni.createFrom().nullItem();
    }

    public record UserInfo(String userId, String email, String displayName) {
    }
}