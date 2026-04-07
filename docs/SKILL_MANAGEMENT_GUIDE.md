# Skill Management Module - Complete Guide

## Overview

The Skill Management module provides comprehensive skill lifecycle management with CRUD operations, seamless repository switching, batch operations, and a REST API.

## Features

### Core Features
- ✅ **CRUD Operations** - Create, Read, Update, Delete skills
- ✅ **Repository Switching** - Seamless switch between file and database
- ✅ **Repository Migration** - Migrate skills between repositories
- ✅ **Repository Sync** - Synchronize skills between repositories
- ✅ **Batch Operations** - Create/delete multiple skills at once
- ✅ **Search & Filter** - Search by query, category, tags
- ✅ **Pagination** - Paginated listing for large skill sets
- ✅ **Lifecycle Management** - Enable/disable skills
- ✅ **Import/Export** - Export/import skills (JSON format)
- ✅ **REST API** - Full RESTful API for all operations
- ✅ **Concurrency Control** - Lock management for safe updates
- ✅ **Validation** - Skill content validation

### Advanced Features
- ✅ **Statistics** - Repository and cache statistics
- ✅ **Cache Management** - Clear cache on demand
- ✅ **Error Handling** - Comprehensive error responses
- ✅ **Audit Trail** - Track skill modifications

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│          SkillManagementResource (REST API)             │
├─────────────────────────────────────────────────────────┤
│           SkillManagementService (Business Logic)       │
│  ├─ CRUD Operations                                     │
│  ├─ Repository Switching                                │
│  ├─ Migration & Sync                                    │
│  ├─ Batch Operations                                    │
│  └─ Lifecycle Management                                │
├─────────────────────────────────────────────────────────┤
│              SkillRepository (SPI)                      │
│  ├─ FileSkillRepository                                 │
│  └─ DatabaseSkillRepository                             │
└─────────────────────────────────────────────────────────┘
```

## Installation

### Maven Dependency

```xml
<dependency>
    <groupId>tech.kayys.gollek</groupId>
    <artifactId>agent-skill-management</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
# application.yaml

gollek:
  agent:
    skills:
      repo:
        type: file  # or "database"
        base-dir: ~/.gollek/skills
        cache:
          ttl: 10m
          max-size: 500
```

## Usage

### Programmatic Usage

```java
@Inject
SkillManagementService skillService;

// Initialize
skillService.initialize(false).await().indefinitely();  // false = use file repo

// Create a skill
SkillContent content = SkillContent.builder()
    .metadata(SkillMetadata.builder()
        .id("my-skill")
        .name("My Skill")
        .version("1.0.0")
        .description("My custom skill")
        .category("CUSTOM")
        .tags(List.of("python", "ml"))
        .build())
    .content("def main():\n    print('Hello')")
    .manifest(Map.of("version", "1.0.0"))
    .build();

SkillMetadata metadata = skillService.createSkill(content)
    .await().indefinitely();

// Get a skill
Optional<SkillContent> skill = skillService.getSkill("my-skill")
    .await().indefinitely();

// Update a skill
SkillContent updated = new SkillContent.Builder()
    .from(content)
    .metadata(new SkillMetadata.Builder()
        .from(content.metadata())
        .version("1.0.1")
        .updatedAt(Instant.now())
        .build())
    .build();

skillService.updateSkill("my-skill", updated).await().indefinitely();

// Delete a skill
boolean deleted = skillService.deleteSkill("my-skill")
    .await().indefinitely();

// Search skills
var results = skillService.searchSkills("python", 0, 10)
    .await().indefinitely();

// Switch repository
skillService.switchRepository("database").await().indefinitely();

// Migrate skills
var stats = skillService.migrateRepository("file", "database")
    .await().indefinitely();

System.out.println("Migrated: " + stats.migrated());
System.out.println("Failed: " + stats.failed());
```

### REST API Usage

#### List Skills
```bash
curl http://localhost:8080/api/skills?page=0&size=20
```

#### Get Skill
```bash
curl http://localhost:8080/api/skills/my-skill
```

#### Create Skill
```bash
curl -X POST http://localhost:8080/api/skills \
  -H "Content-Type: application/json" \
  -d '{
    "metadata": {
      "id": "my-skill",
      "name": "My Skill",
      "version": "1.0.0",
      "description": "My custom skill",
      "category": "CUSTOM"
    },
    "content": "def main(): print(\"Hello\")",
    "manifest": {"version": "1.0.0"}
  }'
```

#### Update Skill
```bash
curl -X PUT http://localhost:8080/api/skills/my-skill \
  -H "Content-Type: application/json" \
  -d '{
    "metadata": {...},
    "content": "...",
    "manifest": {...}
  }'
```

#### Delete Skill
```bash
curl -X DELETE http://localhost:8080/api/skills/my-skill
```

#### Search Skills
```bash
curl "http://localhost:8080/api/skills/search?q=python&page=0&size=10"
```

#### Get by Category
```bash
curl http://localhost:8080/api/skills/category/CODING
```

#### Get by Tags
```bash
curl "http://localhost:8080/api/skills/tags?tags=python&tags=ml&matchAll=true"
```

#### Enable/Disable Skill
```bash
curl -X POST http://localhost:8080/api/skills/my-skill/enable
curl -X POST http://localhost:8080/api/skills/my-skill/disable
```

#### Get Statistics
```bash
curl http://localhost:8080/api/skills/stats
```

#### Get Active Repository
```bash
curl http://localhost:8080/api/skills/repository
```

#### Switch Repository
```bash
curl -X POST http://localhost:8080/api/skills/repository/switch \
  -H "Content-Type: application/json" \
  -d '{"repository": "database"}'
```

#### Migrate Repository
```bash
curl -X POST http://localhost:8080/api/skills/repository/migrate \
  -H "Content-Type: application/json" \
  -d '{"from": "file", "to": "database"}'
```

#### Clear Cache
```bash
curl -X POST http://localhost:8080/api/skills/cache/clear
```

#### Batch Create
```bash
curl -X POST http://localhost:8080/api/skills/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"metadata": {...}, "content": "...", "manifest": {...}},
    {"metadata": {...}, "content": "...", "manifest": {...}}
  ]'
```

#### Batch Delete
```bash
curl -X DELETE http://localhost:8080/api/skills/batch \
  -H "Content-Type: application/json" \
  -d '["skill-1", "skill-2", "skill-3"]'
```

## API Reference

### SkillManagementService Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `initialize(useDatabase)` | Initialize service | `Uni<Void>` |
| `switchRepository(name)` | Switch active repository | `Uni<Void>` |
| `migrateRepository(from, to)` | Migrate skills | `Uni<MigrationStats>` |
| `syncRepositories(repo1, repo2)` | Sync repositories | `Uni<SyncStats>` |
| `createSkill(content)` | Create skill | `Uni<SkillMetadata>` |
| `getSkill(skillId)` | Get skill | `Uni<Optional<SkillContent>>` |
| `updateSkill(skillId, content)` | Update skill | `Uni<SkillMetadata>` |
| `deleteSkill(skillId)` | Delete skill | `Uni<Boolean>` |
| `skillExists(skillId)` | Check existence | `Uni<Boolean>` |
| `createSkills(contents)` | Batch create | `Uni<BatchResult>` |
| `deleteSkills(ids)` | Batch delete | `Uni<BatchResult>` |
| `listSkills()` | List all | `Uni<List<SkillMetadata>>` |
| `listSkills(page, size)` | List paginated | `Uni<PaginatedResult>` |
| `searchSkills(query, page, size)` | Search | `Uni<PaginatedResult>` |
| `getSkillsByCategory(category)` | By category | `Uni<List<SkillMetadata>>` |
| `getSkillsByTags(tags, matchAll)` | By tags | `Uni<List<SkillMetadata>>` |
| `enableSkill(skillId)` | Enable | `Uni<Boolean>` |
| `disableSkill(skillId)` | Disable | `Uni<Boolean>` |
| `getStats()` | Get statistics | `Uni<RepositoryStats>` |
| `clearCache()` | Clear cache | `Uni<Void>` |
| `exportSkill(skillId)` | Export | `Uni<String>` |
| `importSkill(json)` | Import | `Uni<SkillMetadata>` |

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/skills` | List skills |
| GET | `/api/skills/{id}` | Get skill |
| POST | `/api/skills` | Create skill |
| PUT | `/api/skills/{id}` | Update skill |
| DELETE | `/api/skills/{id}` | Delete skill |
| GET | `/api/skills/search` | Search skills |
| GET | `/api/skills/category/{category}` | By category |
| GET | `/api/skills/tags` | By tags |
| POST | `/api/skills/{id}/enable` | Enable skill |
| POST | `/api/skills/{id}/disable` | Disable skill |
| GET | `/api/skills/stats` | Get statistics |
| GET | `/api/skills/repository` | Get repository |
| POST | `/api/skills/repository/switch` | Switch repository |
| POST | `/api/skills/repository/migrate` | Migrate |
| POST | `/api/skills/cache/clear` | Clear cache |
| POST | `/api/skills/batch` | Batch create |
| DELETE | `/api/skills/batch` | Batch delete |

## Repository Switching

### Switch at Runtime

```java
// Switch to database
skillService.switchRepository("database").await().indefinitely();

// Switch to file
skillService.switchRepository("file").await().indefinitely();
```

### Migrate Skills

```java
// Migrate from file to database
MigrationStats stats = skillService.migrateRepository("file", "database")
    .await().indefinitely();

System.out.println("Migrated: " + stats.migrated());
System.out.println("Failed: " + stats.failed());
System.out.println("Skipped: " + stats.skipped());
System.out.println("Total: " + stats.total());
```

### Sync Repositories

```java
// Sync file and database
SyncStats stats = skillService.syncRepositories("file", "database")
    .await().indefinitely();

System.out.println("Added: " + stats.added());
System.out.println("Updated: " + stats.updated());
System.out.println("Source count: " + stats.sourceCount());
System.out.println("Target count: " + stats.targetCount());
```

## Batch Operations

### Batch Create

```java
List<SkillContent> skills = List.of(skill1, skill2, skill3);

BatchResult result = skillService.createSkills(skills)
    .await().indefinitely();

System.out.println("Created: " + result.success());
System.out.println("Failed: " + result.failed());
result.errors().forEach(err -> System.err.println(err));
```

### Batch Delete

```java
List<String> ids = List.of("skill-1", "skill-2", "skill-3");

BatchResult result = skillService.deleteSkills(ids)
    .await().indefinitely();

System.out.println("Deleted: " + result.success());
System.out.println("Failed: " + result.failed());
```

## Error Handling

### Exceptions

- **`SkillAlreadyExistsException`** - Skill ID already exists
- **`SkillNotFoundException`** - Skill not found
- **`IllegalArgumentException`** - Invalid input

### Error Responses

```json
{
  "error": "Skill not found: my-skill"
}
```

HTTP Status Codes:
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Resource deleted
- `400 Bad Request` - Invalid input
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

## Best Practices

### 1. Initialize on Startup

```java
@Startup
public class SkillManagerInitializer {
    @Inject
    SkillManagementService skillService;
    
    @PostConstruct
    void init() {
        skillService.initialize(false).await().indefinitely();
    }
}
```

### 2. Use Transactions for Batch Operations

```java
// Batch operations are automatically transactional
BatchResult result = skillService.createSkills(skills)
    .await().indefinitely();
```

### 3. Handle Concurrency

```java
// Locks are automatically acquired/released
try {
    skillService.updateSkill(skillId, content).await().indefinitely();
} catch (Exception e) {
    // Lock is automatically released
}
```

### 4. Monitor Repository Performance

```java
@Scheduled(every = "1m")
void monitorRepository() {
    RepositoryStats stats = skillService.getStats()
        .await().indefinitely();
    
    if (stats.cacheHitRatio() < 0.5) {
        log.warn("Low cache hit ratio: " + stats.cacheHitRatio());
    }
}
```

### 5. Validate Before Creating

```java
// Validation is automatic, but you can pre-validate
if (skillService.skillExists(skillId).await().indefinitely()) {
    throw new IllegalArgumentException("Skill already exists");
}
```

## Testing

### Unit Tests

```java
@QuarkusTest
class SkillManagementServiceTest {

    @Inject
    SkillManagementService skillService;

    @Test
    void shouldCreateSkill() {
        // Arrange
        SkillContent content = createTestSkill();

        // Act
        SkillMetadata metadata = skillService.createSkill(content)
            .await().indefinitely();

        // Assert
        assertThat(metadata.id()).isEqualTo("test-skill");
    }

    @Test
    void shouldSwitchRepository() {
        // Act
        skillService.switchRepository("database")
            .await().indefinitely();

        // Assert
        assertThat(skillService.getActiveRepositoryName())
            .isEqualTo("database");
    }
}
```

### Integration Tests

```java
@QuarkusTest
class SkillManagementResourceTest {

    @Test
    void testCreateSkill() {
        // Create skill via REST API
        given()
            .contentType(ContentType.JSON)
            .body(createSkillJson())
            .when()
            .post("/api/skills")
            .then()
            .statusCode(201);
    }

    @Test
    void testSearchSkills() {
        given()
            .queryParam("q", "python")
            .when()
            .get("/api/skills/search")
            .then()
            .statusCode(200);
    }
}
```

## Troubleshooting

### Issue: Cannot Create Skill

**Solution**: Check if skill ID already exists
```java
if (skillService.skillExists(skillId).await().indefinitely()) {
    // Use different ID or update existing
}
```

### Issue: Repository Switch Failed

**Solution**: Check repository name
```java
// Valid names: "file", "database"
skillService.switchRepository("file");  // Correct
skillService.switchRepository("FILE");  // Also correct (case-insensitive)
skillService.switchRepository("invalid");  // Throws exception
```

### Issue: Migration Failed

**Solution**: Check source and target repositories
```java
try {
    skillService.migrateRepository("file", "database")
        .await().indefinitely();
} catch (Exception e) {
    log.error("Migration failed", e);
    // Check logs for details
}
```

## Performance

| Operation | Time |
|-----------|------|
| Create skill | 10-100ms |
| Get skill (cache hit) | <1ms |
| Get skill (cache miss) | 5-50ms |
| Update skill | 10-100ms |
| Delete skill | 5-50ms |
| Search | 10-100ms |
| Switch repository | <1ms |
| Migrate (per skill) | 10-100ms |

## Support

- **Guide**: This document
- **API**: `SkillManagementService.java`
- **REST**: `SkillManagementResource.java`
- **Repository**: `SKILLS_REPOSITORY_GUIDE.md`

---

**Version**: 1.0.0
**Last Updated**: 2026-03-28
**Status**: Production Ready
