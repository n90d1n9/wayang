package tech.kayys.wayang.rag.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Schema DTO for a single citation reference in a RAG response.
 */
public class RagCitationSchema {

    @JsonProperty("index")
    @JsonPropertyDescription("1-based index of this citation in the response.")
    private Integer index;

    @JsonProperty("content")
    @JsonPropertyDescription("The cited content excerpt from the source document.")
    private String content;

    @JsonProperty("sourceUri")
    @JsonPropertyDescription("URI or path of the source document.")
    private String sourceUri;

    @JsonProperty("title")
    @JsonPropertyDescription("Title or name of the source document.")
    private String title;

    @JsonProperty("pageNumber")
    @JsonPropertyDescription("Page number within the source where the cited content appears. -1 if unknown.")
    private Integer pageNumber;

    @JsonProperty("sectionTitle")
    @JsonPropertyDescription("Section or heading title where the cited content appears.")
    private String sectionTitle;

    @JsonProperty("confidenceScore")
    @JsonPropertyDescription("Confidence score (0.0–1.0) of the citation's relevance.")
    private Float confidenceScore;

    public RagCitationSchema() {
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Float getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Float confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
