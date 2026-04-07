# Wayang Prompt Module

The Wayang Prompt Module provides a flexible and secure system for managing and rendering AI prompts within the Wayang platform. It supports multiple template rendering strategies, comprehensive validation, and robust security features.

## Features

### Advanced Template Rendering Strategies

The module supports multiple template rendering strategies:

1. **Simple**: Basic `{{variable}}` placeholder replacement
2. **Jinja2**: Jinja2-compatible syntax using Pebble
3. **FreeMarker**: Full Apache FreeMarker support

Each strategy is implemented through the `RenderingEngine` interface and managed by the `RenderingEngineRegistry`.

### Enhanced Validation

- **Template ID Validation**: Ensures template IDs follow the required pattern
- **Variable Definition Validation**: Prevents duplicate variable names
- **Template Body Validation**: Checks for potentially dangerous content
- **Semantic Version Validation**: Ensures version strings follow SemVer format

### Security Enhancements

- **Input Sanitization**: Removes null bytes and other potentially harmful content
- **Sensitive Data Handling**: Proper redaction of sensitive values in audit logs
- **Template Injection Prevention**: Validates template bodies for dangerous patterns

### Flexible Variable Resolution

- **Multiple Sources**: Variables can come from inputs, context, RAG, memory, environment, or secrets
- **Type Coercion**: Automatic conversion between data types
- **Default Values**: Fallback values for optional variables
- **Length Limits**: Configurable maximum lengths for variable values

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  PromptEngine   │───▶│ TemplateRenderer │───▶│ RenderingEngine │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │                           │
                              ▼                           ▼
                       ┌─────────────────┐        ┌─────────────────┐
                       │ VariableResolver│        │ SimpleRendering │
                       └─────────────────┘        │  Engine         │
                                                  └─────────────────┘
                                                          │
                                                          ▼
                                                    ┌─────────────────┐
                                                    │ Jinja2Rendering │
                                                    │  Engine         │
                                                    └─────────────────┘
                                                          │
                                                          ▼
                                                    ┌─────────────────┐
                                                    │ FreeMarker      │
                                                    │  Rendering      │
                                                    │  Engine         │
                                                    └─────────────────┘
```

## Usage Examples

### Creating a Prompt Template

```java
List<PromptVariableDefinition> variables = Arrays.asList(
    new PromptVariableDefinition(
        "userName", 
        "User Name", 
        "The name of the user", 
        PromptVariableDefinition.VariableType.STRING,
        PromptVariableDefinition.VariableSource.INPUT,
        true,  // required
        null,  // no default
        100,   // max length
        false  // not sensitive
    )
);

PromptTemplate template = new PromptTemplate(
    "greeting-template",
    "Greeting Template",
    "A template for greeting users",
    "tenant-123",
    "1.0.0",
    PromptTemplate.TemplateStatus.PUBLISHED,
    Arrays.asList("greeting", "user"),
    Arrays.asList(),
    variables,
    "creator-id",
    Instant.now(),
    "updater-id",
    Instant.now(),
    Map.of("category", "communication")
);
```

### Rendering a Template

```java
// Create a render context with variable values
PromptRenderContext context = new PromptRenderContext.Builder()
    .runId("run-123")
    .nodeId("node-456")
    .tenantId("tenant-123")
    .templateId("greeting-template")
    .inputs(Map.of("userName", "Alice"))
    .build();

// Resolve variables
VariableResolver resolver = new VariableResolver();
List<PromptVariableValue> resolvedVars = resolver.resolve(
    template.getVariableDefinitions(), 
    context
).await().indefinitely();

// Render the template
RenderingEngineRegistry registry = new RenderingEngineRegistry();
registry.initialize();
RenderingEngine engine = registry.forStrategy(PromptVersion.RenderingStrategy.SIMPLE);

String rendered = engine.expand(template.getBody(), resolvedVars);
// Result: "Hello Alice!"
```

## Security Best Practices

1. **Mark Sensitive Variables**: Always set the `sensitive` flag for variables containing confidential information
2. **Validate Input Length**: Use `maxLength` to prevent excessively large inputs
3. **Sanitize External Inputs**: The system automatically sanitizes inputs, but additional validation is recommended
4. **Review Template Bodies**: Ensure templates don't contain malicious code

## Error Handling

The module provides detailed error information through the `PromptEngineException` hierarchy:

- `PromptTemplateNotFoundException`: Template not found
- `PromptValidationException`: Validation failure
- `PromptRenderException`: Rendering failure
- `PromptTokenLimitExceededException`: Token limit exceeded

Each exception contains structured details that map directly to the platform's error handling system.

## Testing

Comprehensive unit tests are provided in the `EnhancedPromptModuleTest` class, covering all new features and security enhancements.