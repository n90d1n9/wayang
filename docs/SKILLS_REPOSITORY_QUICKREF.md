# Skills Repository - Quick Reference

## Quick Start

### 1. Add Dependency

**File-Based** (Default):
```xml
<dependency>
    <groupId>tech.kayys.gollek</groupId>
    <artifactId>agent-skills-repo</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Database**:
```xml
<dependency>
    <groupId>tech.kayys.gollek</groupId>
    <artifactId>agent-skills-repo-db</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure

**File** (`application.yaml`):
```yaml
gollek.agent.skills.repo.type: file
```

**Database**:
```yaml
quarkus.datasource.db-kind: postgresql
gollek.agent.skills.repo.type: database
```

### 3. Use

```java
@Inject
SkillRepository skillRepo;

// Save
skillRepo.save(content).await().indefinitely();

// Get
skillRepo.get("skill-id").await().indefinitely();

// Search
skillRepo.search("query", 0, 10).await().indefinitely();
```

## API Cheat Sheet

| Operation | Method |
|-----------|--------|
| Save skill | `save(content)` |
| Get skill | `get(skillId)` |
| Get metadata | `getMetadata(skillId)` |
| Delete skill | `delete(skillId)` |
| List all | `list()` |
| List page | `list(offset, limit)` |
| Search | `search(query, offset, limit)` |
| By category | `getByCategory(category)` |
| By tags | `getByTags(tags, matchAll)` |
| Exists | `exists(skillId)` |
| Enable/disable | `setEnabled(skillId, enabled)` |
| Stats | `getStats()` |
| Clear cache | `clearCache()` |
| Initialize | `initialize()` |

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `gollek.agent.skills.repo.type` | `file` | `file` or `database` |
| `gollek.agent.skills.repo.base-dir` | `~/.gollek/skills` | File storage path |
| `gollek.agent.skills.repo.cache.ttl` | `10m` | Cache TTL |
| `gollek.agent.skills.repo.cache.max-size` | `500` | Max cache entries |

## File Locations

```
~/.gollek/skills/
├── {skill-id}/
│   ├── manifest.json
│   ├── content.json
│   └── metadata.json
```

## Database Tables

```sql
skills (
    id, skill_id, name, version,
    description, category, author,
    tags (JSONB), content (TEXT),
    manifest_json (JSONB), enabled,
    checksum, created_at, updated_at
)
```

## Performance

| Action | Time |
|--------|------|
| Cache hit | <1ms |
| Cache miss | 5-50ms |
| Save | 10-100ms |
| Search | 10-100ms |

## Common Patterns

### Save Skill
```java
SkillContent content = SkillContent.builder()
    .metadata(SkillMetadata.builder()
        .id("my-skill")
        .name("My Skill")
        .version("1.0.0")
        .description("Description")
        .category("CUSTOM")
        .tags(List.of("tag1", "tag2"))
        .build())
    .content("implementation code")
    .manifest(Map.of("key", "value"))
    .build();

skillRepo.save(content).await().indefinitely();
```

### Search Skills
```java
// By query
skillRepo.search("python", 0, 10).await().indefinitely();

// By category
skillRepo.getByCategory("CODING").await().indefinitely();

// By tags (match any)
skillRepo.getByTags(List.of("python", "ml"), false).await().indefinitely();

// By tags (match all)
skillRepo.getByTags(List.of("python", "ml"), true).await().indefinitely();
```

### Monitor Cache
```java
RepositoryStats stats = skillRepo.getStats().await().indefinitely();
System.out.println("Hit ratio: " + stats.cacheHitRatio());
System.out.println("Total skills: " + stats.totalSkills());
```

### Initialize on Startup
```java
@Startup
public class SkillsInitializer {
    @Inject
    SkillRepository skillRepo;
    
    @PostConstruct
    void init() {
        skillRepo.initialize().await().indefinitely();
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Skills not found | Check `~/.gollek/skills/` or DB connection |
| Low cache hit ratio | Increase `cache.max-size` or `cache.ttl` |
| Slow search | Use `getByCategory()` instead of `search()` |
| Save fails | Check disk space or DB permissions |

## More Info

- **Full Guide**: `SKILLS_REPOSITORY_GUIDE.md`
- **Summary**: `SKILLS_REPOSITORY_SUMMARY.md`
- **SPI**: `agent-skills-repo/src/main/java/.../spi/`
