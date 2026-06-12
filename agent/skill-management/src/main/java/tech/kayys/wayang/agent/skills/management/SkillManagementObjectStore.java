package tech.kayys.wayang.agent.skills.management;

import java.util.List;
import java.util.Optional;

/**
 * Minimal object-store contract for skill-management persistence.
 *
 * <p>Adapters can implement this against S3-compatible stores such as AWS S3,
 * RustFS, MinIO, or a platform storage gateway without pulling provider SDKs
 * into skill-management. The same boundary can back skill definitions,
 * lifecycle state, and event history.
 */
public interface SkillManagementObjectStore {

    Optional<byte[]> get(String key);

    List<String> list(String prefix);

    void put(String key, byte[] content);

    boolean delete(String key);
}
