



```java
/**
 * Example: AI Plugin with 4 different nodes using annotations
 */
@MultiNodePlugin(
    id = "com.acme.ai.nlp",
    name = "AI NLP Plugin",
    version = "1.0.0",
    family = "AI",
    author = "ACME Corp",
    description = "Natural Language Processing nodes"
)
@NodeDefinitions({
    @NodeDefinition(
        type = "ai.sentiment",
        label = "Sentiment Analysis",
        category = "AI",
        subCategory = "NLP",
        description = "Analyze text sentiment",
        icon = "smile",
        color = "#10B981",
        configSchema = "/schemas/sentiment-config.json",
        inputSchema = "/schemas/text-input.json",
        outputSchema = "/schemas/sentiment-output.json",
        executorId = "ai.nlp.executor",
        widgetId = "widget.ai.sentiment"
    ),
    @NodeDefinition(
        type = "ai.classification",
        label = "Text Classification",
        category = "AI",
        subCategory = "NLP",
        description = "Classify text into categories",
        icon = "tag",
        color = "#3B82F6",
        configSchema = "/schemas/classification-config.json",
        inputSchema = "/schemas/text-input.json",
        outputSchema = "/schemas/classification-output.json",
        executorId = "ai.nlp.executor",
        widgetId = "widget.ai.classification"
    ),
    @NodeDefinition(
        type = "ai.ner",
        label = "Named Entity Recognition",
        category = "AI",
        subCategory = "NLP",
        description = "Extract named entities from text",
        icon = "user-check",
        color = "#8B5CF6",
        configSchema = "/schemas/ner-config.json",
        inputSchema = "/schemas/text-input.json",
        outputSchema = "/schemas/ner-output.json",
        executorId = "ai.nlp.executor",
        widgetId = "widget.ai.ner"
    ),
    @NodeDefinition(
        type = "ai.summarization",
        label = "Text Summarization",
        category = "AI",
        subCategory = "NLP",
        description = "Summarize long text",
        icon = "file-text",
        color = "#F59E0B",
        configSchema = "/schemas/summarization-config.json",
        inputSchema = "/schemas/text-input.json",
        outputSchema = "/schemas/summarization-output.json",
        executorId = "ai.nlp.executor",
        widgetId = "widget.ai.summarization"
    )
})
@ApplicationScoped
public class AINLPPlugin {
    
    private static final Logger LOG = Logger.getLogger(AINLPPlugin.class);
    
    public void initialize() {
        LOG.info("AI NLP Plugin initialized with 4 nodes");
    }
}

/**
 * Single executor handling all 4 nodes
 */
@MultiNodeExecutor(
    executorId = "ai.nlp.executor",
    nodeTypes = {
        "ai.sentiment",
        "ai.classification",
        "ai.ner",
        "ai.summarization"
    },
    mode = ExecutionMode.SYNC,
    protocols = {"REST", "GRPC"}
)
@ApplicationScoped
public class AINLPExecutor extends AbstractExecutor {
    
    private static final Logger LOG = Logger.getLogger(AINLPExecutor.class);
    
    @Inject
    SentimentAnalyzer sentimentAnalyzer;
    
    @Inject
    TextClassifier textClassifier;
    
    @Inject
    NamedEntityRecognizer ner;
    
    @Inject
    TextSummarizer summarizer;
    
    @Override
    protected ExecutionResult doExecute(ExecutionRequest request) throws ExecutorException {
        
        String nodeType = request.node().type();
        LOG.infof("Executing node type: %s", nodeType);
        
        // Route to appropriate handler based on node type
        return switch (nodeType) {
            case "ai.sentiment" -> executeSentiment(request);
            case "ai.classification" -> executeClassification(request);
            case "ai.ner" -> executeNER(request);
            case "ai.summarization" -> executeSummarization(request);
            default -> throw new ExecutorException("Unknown node type: " + nodeType);
        };
    }
    
    private ExecutionResult executeSentiment(ExecutionRequest request) {
        String text = request.getInput("text", String.class);
        String model = request.getConfig("model", "default");
        
        SentimentResult result = sentimentAnalyzer.analyze(text, model);
        
        return ExecutionResult.success(Map.of(
            "sentiment", result.sentiment(),
            "score", result.score(),
            "confidence", result.confidence()
        ));
    }
    
    private ExecutionResult executeClassification(ExecutionRequest request) {
        String text = request.getInput("text", String.class);
        List<String> categories = request.getConfig("categories", List.of());
        
        ClassificationResult result = textClassifier.classify(text, categories);
        
        return ExecutionResult.success(Map.of(
            "category", result.category(),
            "confidence", result.confidence(),
            "scores", result.scores()
        ));
    }
    
    private ExecutionResult executeNER(ExecutionRequest request) {
        String text = request.getInput("text", String.class);
        List<String> entityTypes = request.getConfig("entityTypes", List.of());
        
        List<Entity> entities = ner.extract(text, entityTypes);
        
        return ExecutionResult.success(Map.of(
            "entities", entities,
            "count", entities.size()
        ));
    }
    
    private ExecutionResult executeSummarization(ExecutionRequest request) {
        String text = request.getInput("text", String.class);
        int maxLength = request.getConfig("maxLength", 100);
        
        String summary = summarizer.summarize(text, maxLength);
        
        return ExecutionResult.success(Map.of(
            "summary", summary,
            "originalLength", text.length(),
            "summaryLength", summary.length()
        ));
    }
    
    // Service classes
    record SentimentResult(String sentiment, double score, double confidence) {}
    record ClassificationResult(String category, double confidence, Map<String, Double> scores) {}
    record Entity(String text, String type, int start, int end) {}
    
    @ApplicationScoped
    static class SentimentAnalyzer {
        SentimentResult analyze(String text, String model) {
            return new SentimentResult("POSITIVE", 0.85, 0.92);
        }
    }
    
    @ApplicationScoped
    static class TextClassifier {
        ClassificationResult classify(String text, List<String> categories) {
            return new ClassificationResult("Technology", 0.89, Map.of(
                "Technology", 0.89,
                "Business", 0.45,
                "Science", 0.32
            ));
        }
    }
    
    @ApplicationScoped
    static class NamedEntityRecognizer {
        List<Entity> extract(String text, List<String> types) {
            return List.of(
                new Entity("John Doe", "PERSON", 0, 8),
                new Entity("New York", "LOCATION", 25, 33)
            );
        }
    }
    
    @ApplicationScoped
    static class TextSummarizer {
        String summarize(String text, int maxLength) {
            return text.substring(0, Math.min(maxLength, text.length()));
        }
    }
}
```



