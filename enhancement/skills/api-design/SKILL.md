---
name: api-design
description: Design and implement REST APIs, GraphQL schemas, and service interfaces. Activate when the user asks to design an API, create endpoints, define schemas, or implement a service interface.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file write_file search_files glob
---

# API Design Skill

## When to activate
- "design a REST API for X", "create endpoints for Y"
- "implement a service interface", "define the schema"
- "add an endpoint", "design the API contract"
- "write the OpenAPI spec"

## REST API design principles

### Resource naming
- Use nouns, not verbs: `/users` not `/getUsers`
- Plural for collections: `/users`, `/orders`
- Hierarchical for relationships: `/users/{id}/orders`
- Consistent casing: kebab-case for paths

### HTTP method semantics
| Method   | Use case            | Idempotent | Has body |
|----------|---------------------|-----------|---------|
| GET      | Read resource       | Yes       | No      |
| POST     | Create resource     | No        | Yes     |
| PUT      | Full replace        | Yes       | Yes     |
| PATCH    | Partial update      | No        | Yes     |
| DELETE   | Remove resource     | Yes       | No      |

### Response codes
- 200 OK: successful read/update
- 201 Created: successful creation (include Location header)
- 204 No Content: successful delete
- 400 Bad Request: client validation error (include error details)
- 401 Unauthorized: missing/invalid auth
- 403 Forbidden: authenticated but not allowed
- 404 Not Found: resource doesn't exist
- 409 Conflict: state conflict (duplicate, concurrent modification)
- 422 Unprocessable Entity: semantic validation error
- 500 Internal Server Error: unexpected server failure

### Request/Response design
- Consistent envelope: `{"data": ..., "error": null}` or flat (pick one)
- Pagination: `{"items": [...], "cursor": "...", "hasMore": true}`
- Error format: `{"error": {"code": "...", "message": "...", "fields": {...}}}`
- Versioning: URI path (`/v1/`) or header (`Accept: application/vnd.api+json;version=1`)

### OpenAPI spec structure
```yaml
paths:
  /users/{id}:
    get:
      summary: Get user by ID
      parameters: [...]
      responses:
        "200":
          description: User found
          content:
            application/json:
              schema: { $ref: '#/components/schemas/User' }
        "404":
          $ref: '#/components/responses/NotFound'
```

## Service interface design (Java)

```java
public interface UserService {
    // Commands (change state)
    User createUser(CreateUserCommand cmd);
    void updateProfile(UserId id, UpdateProfileCommand cmd);
    void deleteUser(UserId id);

    // Queries (read state)
    Optional<User> findById(UserId id);
    Page<User> search(UserSearchCriteria criteria, Pageable pageable);
}
```

## Implementation checklist
- [ ] Input validation (reject early, describe what's wrong)
- [ ] Authentication & authorisation
- [ ] Rate limiting on write endpoints
- [ ] Idempotency keys for critical mutations
- [ ] Audit log for sensitive operations
- [ ] Error handling with meaningful messages
- [ ] OpenAPI documentation generated from code
- [ ] Unit tests for service layer
- [ ] Integration tests for API layer
