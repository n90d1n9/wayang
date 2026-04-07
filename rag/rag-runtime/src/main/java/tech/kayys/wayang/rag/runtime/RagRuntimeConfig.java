package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class RagRuntimeConfig {
    private String openAiApiKey;
    private String anthropicApiKey;
    private String azureApiKey;
    private String azureEndpoint;
    private String azureChatDeployment;
    private String azureEmbeddingDeployment;
    private boolean logRequests = false;
    private boolean logResponses = false;
    private String vectorstoreBackend = "postgres";
    private int embeddingDimension = 1536;
    private String embeddingModel = "hash-1536";
    private String embeddingVersion = "v1";
    private String ragPluginEnabledIds = "*";
    private String ragPluginOrder = "";
    private String ragPluginTenantEnabledOverrides = "";
    private String ragPluginTenantOrderOverrides = "";
    private String ragPluginSelectionStrategy = "config";
    private boolean ragPluginNormalizeLowercase = false;
    private int ragPluginNormalizeMaxQueryLength = 4096;
    private double ragPluginRerankOriginalWeight = 0.7;
    private double ragPluginRerankLexicalWeight = 0.3;
    private boolean ragPluginRerankAnnotateMetadata = true;
    private String ragPluginSafetyBlockedTerms = "";
    private String ragPluginSafetyMask = "[REDACTED]";
    private String embeddingSchemaHistoryPath;
    private String retrievalEvalHistoryPath;
    private int retrievalEvalHistoryMaxEvents = 1000;
    private String sloAlertSnoozePath;
    private boolean retrievalEvalGuardrailEnabled = true;
    private int retrievalEvalGuardrailWindowSize = 20;
    private double retrievalEvalGuardrailRecallDropMax = 0.05;
    private double retrievalEvalGuardrailMrrDropMax = 0.05;
    private double retrievalEvalGuardrailLatencyP95IncreaseMaxMs = 150.0;
    private double retrievalEvalGuardrailLatencyAvgIncreaseMaxMs = 75.0;

    private String postgresHost = "localhost";
    private int postgresPort = 5432;
    private String postgresDatabase = "gamelan";
    private String postgresUser = "postgres";
    private String postgresPassword;
    private String postgresTable = "embeddings";

    private String pineconeApiKey;
    private String pineconeEnvironment;
    private String pineconeProjectId;
    private String pineconeIndex = "gamelan";

    private String weaviateApiKey;
    private String weaviateScheme = "https";
    private String weaviateHost;
    private String weaviateClassName = "Document";
    private double sloEmbeddingLatencyP95Ms = 800.0;
    private double sloSearchLatencyP95Ms = 1500.0;
    private double sloIngestLatencyP95Ms = 4000.0;
    private double sloEmbeddingFailureRate = 0.05;
    private double sloSearchFailureRate = 0.05;
    private long sloIndexLagMs = 60000L;
    private double sloCompactionFailureRate = 0.10;
    private long sloCompactionCycleStalenessMs = 172800000L;
    private double sloSeverityWarningMultiplier = 1.0;
    private double sloSeverityCriticalMultiplier = 2.0;
    private Map<String, Double> sloSeverityWarningByMetric = Map.of();
    private Map<String, Double> sloSeverityCriticalByMetric = Map.of();
    private boolean sloAlertEnabled = true;
    private String sloAlertMinSeverity = "warning";
    private long sloAlertCooldownMs = 300000L;

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }

    public String getAnthropicApiKey() {
        return anthropicApiKey;
    }

    public void setAnthropicApiKey(String anthropicApiKey) {
        this.anthropicApiKey = anthropicApiKey;
    }

    public String getAzureApiKey() {
        return azureApiKey;
    }

    public void setAzureApiKey(String azureApiKey) {
        this.azureApiKey = azureApiKey;
    }

    public String getAzureEndpoint() {
        return azureEndpoint;
    }

    public void setAzureEndpoint(String azureEndpoint) {
        this.azureEndpoint = azureEndpoint;
    }

    public String getAzureChatDeployment() {
        return azureChatDeployment;
    }

    public void setAzureChatDeployment(String azureChatDeployment) {
        this.azureChatDeployment = azureChatDeployment;
    }

    public String getAzureEmbeddingDeployment() {
        return azureEmbeddingDeployment;
    }

    public void setAzureEmbeddingDeployment(String azureEmbeddingDeployment) {
        this.azureEmbeddingDeployment = azureEmbeddingDeployment;
    }

    public boolean isLogRequests() {
        return logRequests;
    }

    public void setLogRequests(boolean logRequests) {
        this.logRequests = logRequests;
    }

    public boolean isLogResponses() {
        return logResponses;
    }

    public void setLogResponses(boolean logResponses) {
        this.logResponses = logResponses;
    }

    public String getVectorstoreBackend() {
        return vectorstoreBackend;
    }

    public void setVectorstoreBackend(String vectorstoreBackend) {
        this.vectorstoreBackend = vectorstoreBackend;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    public String getRagPluginEnabledIds() {
        return ragPluginEnabledIds;
    }

    public void setRagPluginEnabledIds(String ragPluginEnabledIds) {
        this.ragPluginEnabledIds = ragPluginEnabledIds;
    }

    public String getRagPluginOrder() {
        return ragPluginOrder;
    }

    public void setRagPluginOrder(String ragPluginOrder) {
        this.ragPluginOrder = ragPluginOrder;
    }

    public String getRagPluginTenantEnabledOverrides() {
        return ragPluginTenantEnabledOverrides;
    }

    public void setRagPluginTenantEnabledOverrides(String ragPluginTenantEnabledOverrides) {
        this.ragPluginTenantEnabledOverrides = ragPluginTenantEnabledOverrides;
    }

    public String getRagPluginTenantOrderOverrides() {
        return ragPluginTenantOrderOverrides;
    }

    public void setRagPluginTenantOrderOverrides(String ragPluginTenantOrderOverrides) {
        this.ragPluginTenantOrderOverrides = ragPluginTenantOrderOverrides;
    }

    public String getRagPluginSelectionStrategy() {
        return ragPluginSelectionStrategy;
    }

    public void setRagPluginSelectionStrategy(String ragPluginSelectionStrategy) {
        this.ragPluginSelectionStrategy = ragPluginSelectionStrategy;
    }

    public boolean isRagPluginNormalizeLowercase() {
        return ragPluginNormalizeLowercase;
    }

    public void setRagPluginNormalizeLowercase(boolean ragPluginNormalizeLowercase) {
        this.ragPluginNormalizeLowercase = ragPluginNormalizeLowercase;
    }

    public int getRagPluginNormalizeMaxQueryLength() {
        return ragPluginNormalizeMaxQueryLength;
    }

    public void setRagPluginNormalizeMaxQueryLength(int ragPluginNormalizeMaxQueryLength) {
        this.ragPluginNormalizeMaxQueryLength = ragPluginNormalizeMaxQueryLength;
    }

    public double getRagPluginRerankOriginalWeight() {
        return ragPluginRerankOriginalWeight;
    }

    public void setRagPluginRerankOriginalWeight(double ragPluginRerankOriginalWeight) {
        this.ragPluginRerankOriginalWeight = ragPluginRerankOriginalWeight;
    }

    public double getRagPluginRerankLexicalWeight() {
        return ragPluginRerankLexicalWeight;
    }

    public void setRagPluginRerankLexicalWeight(double ragPluginRerankLexicalWeight) {
        this.ragPluginRerankLexicalWeight = ragPluginRerankLexicalWeight;
    }

    public boolean isRagPluginRerankAnnotateMetadata() {
        return ragPluginRerankAnnotateMetadata;
    }

    public void setRagPluginRerankAnnotateMetadata(boolean ragPluginRerankAnnotateMetadata) {
        this.ragPluginRerankAnnotateMetadata = ragPluginRerankAnnotateMetadata;
    }

    public String getRagPluginSafetyBlockedTerms() {
        return ragPluginSafetyBlockedTerms;
    }

    public void setRagPluginSafetyBlockedTerms(String ragPluginSafetyBlockedTerms) {
        this.ragPluginSafetyBlockedTerms = ragPluginSafetyBlockedTerms;
    }

    public String getRagPluginSafetyMask() {
        return ragPluginSafetyMask;
    }

    public void setRagPluginSafetyMask(String ragPluginSafetyMask) {
        this.ragPluginSafetyMask = ragPluginSafetyMask;
    }

    public String getEmbeddingSchemaHistoryPath() {
        return embeddingSchemaHistoryPath;
    }

    public void setEmbeddingSchemaHistoryPath(String embeddingSchemaHistoryPath) {
        this.embeddingSchemaHistoryPath = embeddingSchemaHistoryPath;
    }

    public String getRetrievalEvalHistoryPath() {
        return retrievalEvalHistoryPath;
    }

    public void setRetrievalEvalHistoryPath(String retrievalEvalHistoryPath) {
        this.retrievalEvalHistoryPath = retrievalEvalHistoryPath;
    }

    public int getRetrievalEvalHistoryMaxEvents() {
        return retrievalEvalHistoryMaxEvents;
    }

    public void setRetrievalEvalHistoryMaxEvents(int retrievalEvalHistoryMaxEvents) {
        this.retrievalEvalHistoryMaxEvents = retrievalEvalHistoryMaxEvents;
    }

    public String getSloAlertSnoozePath() {
        return sloAlertSnoozePath;
    }

    public void setSloAlertSnoozePath(String sloAlertSnoozePath) {
        this.sloAlertSnoozePath = sloAlertSnoozePath;
    }

    public boolean isRetrievalEvalGuardrailEnabled() {
        return retrievalEvalGuardrailEnabled;
    }

    public void setRetrievalEvalGuardrailEnabled(boolean retrievalEvalGuardrailEnabled) {
        this.retrievalEvalGuardrailEnabled = retrievalEvalGuardrailEnabled;
    }

    public int getRetrievalEvalGuardrailWindowSize() {
        return retrievalEvalGuardrailWindowSize;
    }

    public void setRetrievalEvalGuardrailWindowSize(int retrievalEvalGuardrailWindowSize) {
        this.retrievalEvalGuardrailWindowSize = retrievalEvalGuardrailWindowSize;
    }

    public double getRetrievalEvalGuardrailRecallDropMax() {
        return retrievalEvalGuardrailRecallDropMax;
    }

    public void setRetrievalEvalGuardrailRecallDropMax(double retrievalEvalGuardrailRecallDropMax) {
        this.retrievalEvalGuardrailRecallDropMax = retrievalEvalGuardrailRecallDropMax;
    }

    public double getRetrievalEvalGuardrailMrrDropMax() {
        return retrievalEvalGuardrailMrrDropMax;
    }

    public void setRetrievalEvalGuardrailMrrDropMax(double retrievalEvalGuardrailMrrDropMax) {
        this.retrievalEvalGuardrailMrrDropMax = retrievalEvalGuardrailMrrDropMax;
    }

    public double getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs() {
        return retrievalEvalGuardrailLatencyP95IncreaseMaxMs;
    }

    public void setRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(double retrievalEvalGuardrailLatencyP95IncreaseMaxMs) {
        this.retrievalEvalGuardrailLatencyP95IncreaseMaxMs = retrievalEvalGuardrailLatencyP95IncreaseMaxMs;
    }

    public double getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs() {
        return retrievalEvalGuardrailLatencyAvgIncreaseMaxMs;
    }

    public void setRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs(double retrievalEvalGuardrailLatencyAvgIncreaseMaxMs) {
        this.retrievalEvalGuardrailLatencyAvgIncreaseMaxMs = retrievalEvalGuardrailLatencyAvgIncreaseMaxMs;
    }

    public String getPostgresHost() {
        return postgresHost;
    }

    public void setPostgresHost(String postgresHost) {
        this.postgresHost = postgresHost;
    }

    public int getPostgresPort() {
        return postgresPort;
    }

    public void setPostgresPort(int postgresPort) {
        this.postgresPort = postgresPort;
    }

    public String getPostgresDatabase() {
        return postgresDatabase;
    }

    public void setPostgresDatabase(String postgresDatabase) {
        this.postgresDatabase = postgresDatabase;
    }

    public String getPostgresUser() {
        return postgresUser;
    }

    public void setPostgresUser(String postgresUser) {
        this.postgresUser = postgresUser;
    }

    public String getPostgresPassword() {
        return postgresPassword;
    }

    public void setPostgresPassword(String postgresPassword) {
        this.postgresPassword = postgresPassword;
    }

    public String getPostgresTable() {
        return postgresTable;
    }

    public void setPostgresTable(String postgresTable) {
        this.postgresTable = postgresTable;
    }

    public String getPineconeApiKey() {
        return pineconeApiKey;
    }

    public void setPineconeApiKey(String pineconeApiKey) {
        this.pineconeApiKey = pineconeApiKey;
    }

    public String getPineconeEnvironment() {
        return pineconeEnvironment;
    }

    public void setPineconeEnvironment(String pineconeEnvironment) {
        this.pineconeEnvironment = pineconeEnvironment;
    }

    public String getPineconeProjectId() {
        return pineconeProjectId;
    }

    public void setPineconeProjectId(String pineconeProjectId) {
        this.pineconeProjectId = pineconeProjectId;
    }

    public String getPineconeIndex() {
        return pineconeIndex;
    }

    public void setPineconeIndex(String pineconeIndex) {
        this.pineconeIndex = pineconeIndex;
    }

    public String getWeaviateApiKey() {
        return weaviateApiKey;
    }

    public void setWeaviateApiKey(String weaviateApiKey) {
        this.weaviateApiKey = weaviateApiKey;
    }

    public String getWeaviateScheme() {
        return weaviateScheme;
    }

    public void setWeaviateScheme(String weaviateScheme) {
        this.weaviateScheme = weaviateScheme;
    }

    public String getWeaviateHost() {
        return weaviateHost;
    }

    public void setWeaviateHost(String weaviateHost) {
        this.weaviateHost = weaviateHost;
    }

    public String getWeaviateClassName() {
        return weaviateClassName;
    }

    public void setWeaviateClassName(String weaviateClassName) {
        this.weaviateClassName = weaviateClassName;
    }

    public double getSloEmbeddingLatencyP95Ms() {
        return sloEmbeddingLatencyP95Ms;
    }

    public void setSloEmbeddingLatencyP95Ms(double sloEmbeddingLatencyP95Ms) {
        this.sloEmbeddingLatencyP95Ms = sloEmbeddingLatencyP95Ms;
    }

    public double getSloSearchLatencyP95Ms() {
        return sloSearchLatencyP95Ms;
    }

    public void setSloSearchLatencyP95Ms(double sloSearchLatencyP95Ms) {
        this.sloSearchLatencyP95Ms = sloSearchLatencyP95Ms;
    }

    public double getSloIngestLatencyP95Ms() {
        return sloIngestLatencyP95Ms;
    }

    public void setSloIngestLatencyP95Ms(double sloIngestLatencyP95Ms) {
        this.sloIngestLatencyP95Ms = sloIngestLatencyP95Ms;
    }

    public double getSloEmbeddingFailureRate() {
        return sloEmbeddingFailureRate;
    }

    public void setSloEmbeddingFailureRate(double sloEmbeddingFailureRate) {
        this.sloEmbeddingFailureRate = sloEmbeddingFailureRate;
    }

    public double getSloSearchFailureRate() {
        return sloSearchFailureRate;
    }

    public void setSloSearchFailureRate(double sloSearchFailureRate) {
        this.sloSearchFailureRate = sloSearchFailureRate;
    }

    public long getSloIndexLagMs() {
        return sloIndexLagMs;
    }

    public void setSloIndexLagMs(long sloIndexLagMs) {
        this.sloIndexLagMs = sloIndexLagMs;
    }

    public double getSloCompactionFailureRate() {
        return sloCompactionFailureRate;
    }

    public void setSloCompactionFailureRate(double sloCompactionFailureRate) {
        this.sloCompactionFailureRate = sloCompactionFailureRate;
    }

    public long getSloCompactionCycleStalenessMs() {
        return sloCompactionCycleStalenessMs;
    }

    public void setSloCompactionCycleStalenessMs(long sloCompactionCycleStalenessMs) {
        this.sloCompactionCycleStalenessMs = sloCompactionCycleStalenessMs;
    }

    public double getSloSeverityWarningMultiplier() {
        return sloSeverityWarningMultiplier;
    }

    public void setSloSeverityWarningMultiplier(double sloSeverityWarningMultiplier) {
        this.sloSeverityWarningMultiplier = sloSeverityWarningMultiplier;
    }

    public double getSloSeverityCriticalMultiplier() {
        return sloSeverityCriticalMultiplier;
    }

    public void setSloSeverityCriticalMultiplier(double sloSeverityCriticalMultiplier) {
        this.sloSeverityCriticalMultiplier = sloSeverityCriticalMultiplier;
    }

    public Map<String, Double> getSloSeverityWarningByMetric() {
        return sloSeverityWarningByMetric;
    }

    public void setSloSeverityWarningByMetric(Map<String, Double> sloSeverityWarningByMetric) {
        this.sloSeverityWarningByMetric = sloSeverityWarningByMetric == null ? Map.of()
                : Map.copyOf(sloSeverityWarningByMetric);
    }

    public Map<String, Double> getSloSeverityCriticalByMetric() {
        return sloSeverityCriticalByMetric;
    }

    public void setSloSeverityCriticalByMetric(Map<String, Double> sloSeverityCriticalByMetric) {
        this.sloSeverityCriticalByMetric = sloSeverityCriticalByMetric == null ? Map.of()
                : Map.copyOf(sloSeverityCriticalByMetric);
    }

    public boolean isSloAlertEnabled() {
        return sloAlertEnabled;
    }

    public void setSloAlertEnabled(boolean sloAlertEnabled) {
        this.sloAlertEnabled = sloAlertEnabled;
    }

    public String getSloAlertMinSeverity() {
        return sloAlertMinSeverity;
    }

    public void setSloAlertMinSeverity(String sloAlertMinSeverity) {
        this.sloAlertMinSeverity = sloAlertMinSeverity;
    }

    public long getSloAlertCooldownMs() {
        return sloAlertCooldownMs;
    }

    public void setSloAlertCooldownMs(long sloAlertCooldownMs) {
        this.sloAlertCooldownMs = sloAlertCooldownMs;
    }
}
