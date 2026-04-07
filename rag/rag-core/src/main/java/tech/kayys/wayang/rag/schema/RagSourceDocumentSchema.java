package tech.kayys.wayang.rag.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;

/**
 * Schema DTO for a source document retrieved in a RAG operation.
 */
public class RagSourceDocumentSchema {

    @JsonProperty("id")
    @JsonPropertyDescription("Unique identifier of the source document or chunk.")
    private String id;

    @JsonProperty("title")
    @JsonPropertyDescription("Title or name of the source document.")
    private String title;

    @JsonProperty("content")
    @JsonPropertyDescription("Text content of the retrieved chunk.")
    private String content;

    @JsonProperty("sourceUri")
    @JsonPropertyDescription("URI or path of the source document.")
    private String sourceUri;

    @JsonProperty("metadata")
    @JsonPropertyDescription("Key-value metadata associated with the document.")
    private Map<String, String> metadata;

    @JsonProperty("similarityScore")
    @JsonPropertyDescription("Cosine similarity score (0.0–1.0) between the query and this document.")
    private Float similarityScore;

    @JsonProperty("pageNumber")
    @JsonPropertyDescription("Page number within the source. -1 if unknown.")
    private Integer pageNumber;

    @JsonProperty("sectionTitle")
    @JsonPropertyDescription("Section or heading title where the chunk appears.")
    private String sectionTitle;

    public RagSourceDocumentSchema() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Float getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(Float similarityScore) {
        this.similarityScore = similarityScore;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
