# Skills Repository - Complete Guide

## Overview

The Skills Repository provides persistent storage for agent skills with automatic caching, supporting both file-based and database-backed implementations.

## Features

### File-Based Repository (`agent-skills-repo`)
- ✅ Stores skills in `~/.gollek/skills/`
- ✅ L1/L2 caching (Caffeine + File System)
- ✅ No database required
- ✅ Default implementation
- ✅ Automatic directory management

### Database Repository (`agent-skills-repo-db`)
- ✅ PostgreSQL storage with Hibernate Reactive
- ✅ L1 caching (Caffeine)
- ✅ Full-text search
- ✅ Transactional operations
- ✅ Scalable for production

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              SkillRepository (SPI)                      │
├─────────────────────────────────────────────────────────┤
│  FileSkillRepository          DatabaseSkillRepository   │
│  ├─ L1 Cache (Caffeine)       ├─ L1 Cache (Caffeine)   │
│  ├─ L2 Cache (File System)    ├─ Database (PostgreSQL) │
│  └─ ~/.gollek/skills/         └─ skills table          │
└─────────────────────────────────────────────────────────┘
```

## Installation

### Maven Dependencies

#### File-Based (Default)
```xml
<dependency>
    <groupId>tech.kayys.gollek</groupId>
    <artifactId>agent-skills-repo</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Database-Backed
```xml
<dependency>
    <groupId>tech.kayys.gollek</groupId>
    <artifactId>agent-skills-repo-db</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### File-Based Repository

```yaml
gollek:
  agent:
    skills:
      repo:
        type: file  # default
        base-dir: ~/.gollek/skills
        cache:
          ttl: 10m
          max-size: 500
```

### Database Repository

```yaml
quarkus:
  datasource:
    db-kind: postgresql
    username: gollek
    password: ${DB_PASSWORD}
    url: jdbc:postgresql://localhost:5432/gollek

gollek:
  agent:
    skills:
      repo:
        type: database
        cache:
          ttl: 10m
          max-size: 500
```

## Usage

### Basic Operations

```java
@Inject
SkillRepository skillRepo;

// Initialize repository
skillRepo.initialize().await().indefinitely();

// Save a skill
SkillContent content = SkillContent.builder()
    .metadata(SkillMetadata.builder()
        .id("my-skill")
        .name("My Skill")
        .version("1.0.0")
        .description("My custom skill")
        .category("CUSTOM")
        .build())
    .content("skill implementation code")
    .manifest(Map.of("version", "1.0.0"))
    .build();

skillRepo.save(content).await().indefinitely();

// Get a skill
Optional<SkillContent> skill = skillRepo.get("my-skill")
    .await().indefinitely();

// Get metadata only
Optional<SkillMetadata> metadata = skillRepo.getMetadata("my-skill")
    .await().indefinitely();

// Delete a skill
boolean deleted = skillRepo.delete("my-skill")
    .await().indefinitely();
```

### Search Operations

```java
// List all skills
List<SkillMetadata> skills = skillRepo.list()
    .await().indefinitely();

// List with pagination
List<SkillMetadata> page = skillRepo.list(0, 20)
    .await().indefinitely();

// Search by query
List<SkillMetadata> results = skillRepo.search("python", 0, 10)
    .await().indefinitely();

// Get by category
List<SkillMetadata> coding = skillRepo.getByCategory("CODING")
    .await().indefinitely();

// Get by tags
List<SkillMetadata> tagged = skillRepo.getByTags(
    List.of("python", "ml"), true  // match all
).await().indefinitely();
```

### Advanced Operations

```java
// Check existence
boolean exists = skillRepo.exists("my-skill")
    .await().indefinitely();

// Enable/disable
skillRepo.setEnabled("my-skill", false)
    .await().indefinitely();

// Get statistics
RepositoryStats stats = skillRepo.getStats()
    .await().indefinitely();

System.out.println("Total skills: " + stats.totalSkills());
System.out.println("Cache hit ratio: " + stats.cacheHitRatio());

// Clear cache
skillRepo.clearCache().await().indefinitely();
```

## File Structure (File-Based)

```
~/.gollek/skills/
├── my-skill/
│   ├── manifest.json    # Skill manifest
│   ├── content.json     # Skill implementation
│   └── metadata.json    # Skill metadata
├── python-coder/
│   ├── manifest.json
│   ├── content.json
│   └── metadata.json
└── ...
```

### Example Files

**metadata.json**:
```json
{
  "id": "my-skill",
  "name": "My Skill",
  "version": "1.0.0",
  "description": "My custom skill",
  "category": "CUSTOM",
  "author": "John Doe",
  "tags": ["python", "ml"],
  "contentPath": "/home/user/.gollek/skills/my-skill/content.json",
  "manifestPath": "/home/user/.gollek/skills/my-skill/manifest.json",
  "createdAt": "2026-03-28T10:00:00Z",
  "updatedAt": "2026-03-28T10:00:00Z",
  "enabled": true,
  "checksum": "abc123..."
}
```

**manifest.json**:
```json
{
  "version": "1.0.0",
  "dependencies": ["numpy", "pandas"],
  "entryPoint": "main.py",
  "config": {
    "timeout": 30,
    "maxRetries": 3
  }
}
```

**content.json**:
```json
{
  "metadata": { ... },
  "content": "def main():\n    print('Hello')",
  "manifest": { ... }
}
```

## Database Schema (Database Repository)

```sql
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    skill_id VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50),
    description TEXT,
    category VARCHAR(100),
    author VARCHAR(255),
    tags JSONB,
    content TEXT,
    manifest_json JSONB,
    enabled BOOLEAN DEFAULT true,
    checksum VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_skills_tags ON skills USING GIN(tags);
CREATE INDEX idx_skills_enabled ON skills(enabled);
```

## Caching

### L1 Cache (Caffeine)
- In-memory cache
- Configurable TTL (default: 10 minutes)
- Configurable max size (default: 500 entries)
- Automatic eviction

### L2 Cache (File-Based Only)
- File system storage
- Persistent across restarts
- Automatic loading

### Cache Performance

```java
RepositoryStats stats = skillRepo.getStats().await().indefinitely();

System.out.println("Cache hits: " + stats.cacheHits());
System.out.println("Cache misses: " + stats.cacheMisses());
System.out.println("Hit ratio: " + stats.cacheHitRatio());  // Target: >0.7
```

### Expected Performance

| Operation | File-Based | Database |
|-----------|------------|----------|
| Get (cache hit) | <1ms | <1ms |
| Get (cache miss) | 5-10ms | 10-50ms |
| Save | 10-20ms | 20-100ms |
| Search | 50-100ms | 10-50ms |
| Cache hit ratio | ~80% | ~80% |

## Implementation Selection

### Use File-Based When:
- ✅ Development/testing
- ✅ Single-user scenarios
- ✅ No database available
- ✅ Simple deployment
- ✅ <1000 skills

### Use Database When:
- ✅ Production environment
- ✅ Multi-user scenarios
- ✅ Need transactions
- ✅ Need advanced search
- ✅ >1000 skills
- ✅ Need backup/restore

## Best Practices

### 1. Initialize on Startup
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

### 2. Use Metadata for Listing
```java
// Good: Get metadata only (faster)
List<SkillMetadata> skills = skillRepo.list().await().indefinitely();

// Bad: Get full content for listing
List<SkillContent> skills = skillRepo.list()
    .await().indefinitely()
    .stream()
    .map(m -> skillRepo.get(m.id()).await().indefinitely())
    .toList();
```

### 3. Enable Caching
```java
// Cache is automatic, but monitor performance
@Scheduled(every = "1m")
void monitorCache() {
    RepositoryStats stats = skillRepo.getStats().await().indefinitely();
    if (stats.cacheHitRatio() < 0.5) {
        log.warn("Low cache hit ratio: " + stats.cacheHitRatio());
    }
}
```

### 4. Use Search Efficiently
```java
// Good: Use specific search
List<SkillMetadata> results = skillRepo.search("python coding", 0, 10)
    .await().indefinitely();

// Better: Use category filter
List<SkillMetadata> results = skillRepo.getByCategory("CODING")
    .await().indefinitely();
```

### 5. Handle Errors Gracefully
```java
try {
    Optional<SkillContent> skill = skillRepo.get("my-skill")
        .await().atMost(Duration.ofSeconds(5));
    
    if (skill.isEmpty()) {
        log.warn("Skill not found: my-skill");
        // Handle missing skill
    }
} catch (Exception e) {
    log.error("Failed to get skill", e);
    // Use fallback or return error
}
```

## Migration

### From File to Database

```java
// Migrate skills from file to database
SkillRepository fileRepo = new FileSkillRepository();
SkillRepository dbRepo = new DatabaseSkillRepository();

fileRepo.list().await().indefinitely().forEach(metadata -> {
    fileRepo.get(metadata.id()).await().indefinitely()
        .ifPresent(content -> {
            dbRepo.save(content).await().indefinitely();
        });
});
```

## Testing

### Unit Tests
```java
@QuarkusTest
class SkillRepositoryTest {

    @Inject
    SkillRepository skillRepo;

    @Test
    void shouldSaveAndGetSkill() {
        // Arrange
        SkillContent content = createTestSkill();

        // Act
        skillRepo.save(content).await().indefinitely();
        Optional<SkillContent> retrieved = skillRepo.get("test-skill")
            .await().indefinitely();

        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().metadata().id()).isEqualTo("test-skill");
    }

    @Test
    void shouldSearchSkills() {
        // Arrange
        createTestSkills();

        // Act
        List<SkillMetadata> results = skillRepo.search("python", 0, 10)
            .await().indefinitely();

        // Assert
        assertThat(results).isNotEmpty();
    }
}
```

## Troubleshooting

### Issue: Skills Not Found

**Solution**: Check base directory or database connection
```bash
# File-based
ls -la ~/.gollek/skills/

# Database
psql -U gollek -d gollek -c "SELECT * FROM skills;"
```

### Issue: Low Cache Hit Ratio

**Solution**: Increase cache size or TTL
```yaml
gollek:
  agent:
    skills:
      repo:
        cache:
          max-size: 1000  # Increase from 500
          ttl: 30m        # Increase from 10m
```

### Issue: Slow Search

**Solution**: Use category filter or database repository
```java
// Instead of search
skillRepo.search("coding", 0, 10);

// Use category filter
skillRepo.getByCategory("CODING");
```

## API Reference

### SkillRepository Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `save(content)` | Save skill | `Uni<SkillMetadata>` |
| `get(skillId)` | Get skill content | `Uni<Optional<SkillContent>>` |
| `getMetadata(skillId)` | Get skill metadata | `Uni<Optional<SkillMetadata>>` |
| `delete(skillId)` | Delete skill | `Uni<Boolean>` |
| `list(offset, limit)` | List skills | `Uni<List<SkillMetadata>>` |
| `search(query, offset, limit)` | Search skills | `Uni<List<SkillMetadata>>` |
| `getByCategory(category)` | Get by category | `Uni<List<SkillMetadata>>` |
| `getByTags(tags, matchAll)` | Get by tags | `Uni<List<SkillMetadata>>` |
| `exists(skillId)` | Check existence | `Uni<Boolean>` |
| `setEnabled(skillId, enabled)` | Enable/disable | `Uni<Boolean>` |
| `getStats()` | Get statistics | `Uni<RepositoryStats>` |
| `clearCache()` | Clear cache | `Uni<Void>` |
| `initialize()` | Initialize repo | `Uni<Void>` |

## Support

- **Issues**: GitHub Issues
- **Documentation**: See other docs in `gollek-extension/agent/docs/`
- **Examples**: See test classes

---

**Version**: 1.0.0
**Last Updated**: 2026-03-28
**Status**: Production Ready
