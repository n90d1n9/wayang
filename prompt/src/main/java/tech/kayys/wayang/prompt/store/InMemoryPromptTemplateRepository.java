package tech.kayys.wayang.prompt.store;

import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.prompt.core.PromptTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback repository used when Hibernate ORM is disabled (e.g. standalone tests).
 */
@ApplicationScoped
@IfBuildProperty(name = "quarkus.hibernate-orm.enabled", stringValue = "false")
public class InMemoryPromptTemplateRepository implements PromptTemplateRepository {

    private final Map<String, PromptTemplate> store = new ConcurrentHashMap<>();

    @Override
    public Uni<PromptTemplate> save(PromptTemplate template) {
        if (template == null) {
            return Uni.createFrom().item((PromptTemplate) null);
        }
        store.put(key(template.getTemplateId(), template.getTenantId()), template);
        return Uni.createFrom().item(template);
    }

    @Override
    public Uni<PromptTemplate> findById(String templateId, String tenantId) {
        return Uni.createFrom().item(store.get(key(templateId, tenantId)));
    }

    @Override
    public Uni<List<PromptTemplate>> findByTenant(String tenantId, int page, int pageSize) {
        List<PromptTemplate> matches = store.values().stream()
                .filter(template -> tenantId != null && tenantId.equals(template.getTenantId()))
                .toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(pageSize, 1);
        int from = Math.min(safePage * safeSize, matches.size());
        int to = Math.min(from + safeSize, matches.size());
        return Uni.createFrom().item(new ArrayList<>(matches.subList(from, to)));
    }

    @Override
    public Uni<Void> delete(String templateId, String tenantId) {
        store.remove(key(templateId, tenantId));
        return Uni.createFrom().voidItem();
    }

    private static String key(String templateId, String tenantId) {
        return (tenantId == null ? "" : tenantId) + "::" + (templateId == null ? "" : templateId);
    }
}
