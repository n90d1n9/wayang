package tech.kayys.wayang.agent.skills.management;

import io.smallrye.mutiny.Uni;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Centralizes Mutiny adaptation for synchronous skill-management workflows.
 */
final class SkillManagementAsync {

    private SkillManagementAsync() {
    }

    static <T> Uni<T> item(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return Uni.createFrom().item(supplier);
    }
}
