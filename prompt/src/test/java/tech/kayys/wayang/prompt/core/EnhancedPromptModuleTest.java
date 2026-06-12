package tech.kayys.wayang.prompt.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the enhanced prompt module features.
 */
class EnhancedPromptModuleTest {

    @Nested
    class RenderingEngineTest {
        
        @Test
        void testSimpleRenderingEngine() {
            SimpleRenderingEngine engine = new SimpleRenderingEngine();
            assertEquals(PromptVersion.RenderingStrategy.SIMPLE, engine.getStrategy());
            
            List<PromptVariableValue> vars = List.of(
                new PromptVariableValue("name", "John", PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis()),
                new PromptVariableValue("age", 30, PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis())
            );
            
            String result = engine.expand("Hello {{name}}, you are {{age}} years old.", vars);
            assertEquals("Hello John, you are 30 years old.", result);
        }
        
        @Test
        void testSimpleRenderingEngineWithMissingVariable() {
            SimpleRenderingEngine engine = new SimpleRenderingEngine();
            
            List<PromptVariableValue> vars = List.of(
                new PromptVariableValue("name", "John", PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis())
            );
            
            // When a variable is not found, it should remain in the template
            String result = engine.expand("Hello {{name}}, you are {{age}} years old.", vars);
            assertEquals("Hello John, you are {{age}} years old.", result);
        }
        
        @Test
        void testJinja2RenderingEngine() {
            Jinja2RenderingEngine engine = new Jinja2RenderingEngine();
            assertEquals(PromptVersion.RenderingStrategy.JINJA2, engine.getStrategy());

            List<PromptVariableValue> vars = List.of(
                new PromptVariableValue("name", "John", PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis()),
                new PromptVariableValue("items", List.of("apple", "banana"), PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis())
            );

            String result = engine.expand("Hello {{ name }}: {% for item in items %}{{ item }}{% if not loop.last %}, {% endif %}{% endfor %}.", vars);
            assertEquals("Hello John: apple, banana.", result);
        }
        
        @Test
        void testFreeMarkerRenderingEngine() {
            FreeMarkerRenderingEngine engine = new FreeMarkerRenderingEngine();
            assertEquals(PromptVersion.RenderingStrategy.FREEMARKER, engine.getStrategy());

            List<PromptVariableValue> vars = List.of(
                new PromptVariableValue("name", "John", PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis()),
                new PromptVariableValue("items", List.of("apple", "banana"), PromptVariableDefinition.VariableSource.INPUT, false, System.currentTimeMillis())
            );

            String result = engine.expand("Hello ${name}: <#list items as item>${item}<#sep>, </#list>.", vars);
            assertEquals("Hello John: apple, banana.", result);
        }
    }
    
    @Nested
    class PromptTemplateValidationTest {
        
        @Test
        void testPromptTemplateWithDuplicateVariableDefinitions() {
            List<PromptVariableDefinition> duplicateVars = List.of(
                new PromptVariableDefinition("var1", "Var 1", "Description", VariableType.STRING,
                    PromptVariableDefinition.VariableSource.INPUT, true, null, null, false),
                new PromptVariableDefinition("var1", "Var 1 Duplicate", "Description", VariableType.STRING, 
                    PromptVariableDefinition.VariableSource.INPUT, true, null, null, false)
            );
            
            // Should throw an exception for duplicate variable names
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                new PromptTemplate(
                    "test/duplicate-vars",
                    "Test Template",
                    "A template with duplicate variables",
                    "tenant1",
                    "1.0.0",
                    PromptTemplate.TemplateStatus.PUBLISHED,
                    List.of("test", "validation"),
                    List.of(),
                    duplicateVars,
                    "creator",
                    java.time.Instant.now(),
                    "updater",
                    java.time.Instant.now(),
                    Map.of("key", "value")
                );
            });
            
            assertTrue(ex.getMessage().contains("Duplicate variable definition found"));
        }
        
        @Test
        void testPromptTemplateWithValidVariableDefinitions() {
            List<PromptVariableDefinition> uniqueVars = List.of(
                new PromptVariableDefinition("var1", "Var 1", "Description", VariableType.STRING,
                    PromptVariableDefinition.VariableSource.INPUT, true, null, null, false),
                new PromptVariableDefinition("var2", "Var 2", "Description", VariableType.STRING,
                    PromptVariableDefinition.VariableSource.INPUT, true, null, null, false)
            );
            
            // Should not throw an exception
            assertDoesNotThrow(() -> {
                new PromptTemplate(
                    "test/unique-vars",
                    "Test Template",
                    "A template with unique variables",
                    "tenant1",
                    "1.0.0",
                    PromptTemplate.TemplateStatus.PUBLISHED,
                    List.of("test", "validation"),
                    List.of(),
                    uniqueVars,
                    "creator",
                    java.time.Instant.now(),
                    "updater",
                    java.time.Instant.now(),
                    Map.of("key", "value")
                );
            });
        }
    }
    
    @Nested
    class PromptVersionValidationTest {
        
        @Test
        void testPromptVersionWithDangerousTemplateBody() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                new PromptVersion(
                    "1.0.0",
                    "<script>alert('xss')</script>",
                    "System prompt",
                    PromptVersion.RenderingStrategy.SIMPLE,
                    100,
                    1000,
                    PromptVersion.VersionStatus.DRAFT,
                    "hash123",
                    "creator",
                    java.time.Instant.now(),
                    Map.of()
                );
            });
            
            assertTrue(ex.getMessage().contains("contains potentially dangerous script tag"));
        }
        
        @Test
        void testPromptVersionWithSafeTemplateBody() {
            // Should not throw an exception
            assertDoesNotThrow(() -> {
                new PromptVersion(
                    "1.0.0",
                    "This is a safe template body without dangerous content.",
                    "System prompt",
                    PromptVersion.RenderingStrategy.SIMPLE,
                    100,
                    1000,
                    PromptVersion.VersionStatus.DRAFT,
                    "hash123",
                    "creator",
                    java.time.Instant.now(),
                    Map.of()
                );
            });
        }
    }
    
    @Nested
    class VariableResolverSecurityTest {
        
        @Test
        void testVariableResolverSanitizesNullBytes() {
            VariableResolver resolver = new VariableResolver();
            
            // Create a variable definition
            List<PromptVariableDefinition> defs = List.of(
                new PromptVariableDefinition("testVar", "Test Var", "A test variable", 
                    VariableType.STRING,
                    PromptVariableDefinition.VariableSource.INPUT, 
                    false, null, 100, false)
            );
            
            // Create a context with a value containing null bytes
            PromptRenderContext context = new PromptRenderContext.Builder()
                .runId("run123")
                .nodeId("node123")
                .tenantId("tenant1")
                .templateId("test/template")
                .inputs(Map.of("testVar", "Hello\0World"))
                .build();
            
            // Resolve the variables
            var result = resolver.resolve(defs, context).await().indefinitely();
            
            // Check that the null byte was sanitized
            assertEquals(1, result.size());
            assertEquals("HelloWorld", result.get(0).getValue());
        }
        
        @Test
        void testVariableResolverHandlesNormalStrings() {
            VariableResolver resolver = new VariableResolver();
            
            // Create a variable definition
            List<PromptVariableDefinition> defs = List.of(
                new PromptVariableDefinition("testVar", "Test Var", "A test variable", 
                    VariableType.STRING,
                    PromptVariableDefinition.VariableSource.INPUT, 
                    false, null, 100, false)
            );
            
            // Create a context with a normal value
            PromptRenderContext context = new PromptRenderContext.Builder()
                .runId("run123")
                .nodeId("node123")
                .tenantId("tenant1")
                .templateId("test/template")
                .inputs(Map.of("testVar", "Hello World"))
                .build();
            
            // Resolve the variables
            var result = resolver.resolve(defs, context).await().indefinitely();
            
            // Check that the value is preserved
            assertEquals(1, result.size());
            assertEquals("Hello World", result.get(0).getValue());
        }
    }
    
    @Nested
    class RenderingEngineRegistryTest {
        
        @Test
        void testRenderingEngineRegistryInitialization() {
            RenderingEngineRegistry registry = new RenderingEngineRegistry();
            registry.initialize();
            
            // Should have at least the simple engine
            RenderingEngine simpleEngine = registry.forStrategy(PromptVersion.RenderingStrategy.SIMPLE);
            assertNotNull(simpleEngine);
            assertEquals(SimpleRenderingEngine.class, simpleEngine.getClass());
        }
        
        @Test
        void testRenderingEngineRegistryForKnownStrategies() {
            RenderingEngineRegistry registry = new RenderingEngineRegistry();
            registry.initialize();
            
            // Test that we can get engines for all known strategies
            for (PromptVersion.RenderingStrategy strategy : PromptVersion.RenderingStrategy.values()) {
                if (strategy == PromptVersion.RenderingStrategy.SIMPLE) {
                    RenderingEngine engine = registry.forStrategy(strategy);
                    assertNotNull(engine);
                    assertEquals(strategy, engine.getStrategy());
                }
                // For other strategies, we expect them to be available if their dependencies are present
            }
        }
        
        @Test
        void testRenderingEngineRegistryForAllStrategies() {
            RenderingEngineRegistry registry = new RenderingEngineRegistry();
            registry.initialize();

            // Test that SIMPLE strategy is always available
            RenderingEngine simpleEngine = registry.forStrategy(PromptVersion.RenderingStrategy.SIMPLE);
            assertNotNull(simpleEngine);
            assertEquals(PromptVersion.RenderingStrategy.SIMPLE, simpleEngine.getStrategy());
            
            RenderingEngine jinja2Engine = registry.forStrategy(PromptVersion.RenderingStrategy.JINJA2);
            assertNotNull(jinja2Engine);
            assertEquals(PromptVersion.RenderingStrategy.JINJA2, jinja2Engine.getStrategy());

            RenderingEngine freeMarkerEngine = registry.forStrategy(PromptVersion.RenderingStrategy.FREEMARKER);
            assertNotNull(freeMarkerEngine);
            assertEquals(PromptVersion.RenderingStrategy.FREEMARKER, freeMarkerEngine.getStrategy());
        }
    }
}
