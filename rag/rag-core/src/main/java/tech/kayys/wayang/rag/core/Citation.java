package tech.kayys.wayang.rag.core;

/**
 * Represents a citation for a piece of information generated in a RAG response.
 */
public class Citation {
    private final int index;
    private final String content;
    private final String sourceUri;
    private final String title;
    private final int pageNumber;
    private final String sectionTitle;
    private final float confidenceScore;

    public Citation(String content, String sourceUri, int index, java.util.Map<String, Object> metadata) {
        this.index = index;
        this.content = content;
        this.sourceUri = sourceUri;
        this.title = (String) metadata.getOrDefault("title", "");

        int pNum = -1;
        Object pageNumObj = metadata.get("pageNumber");
        if (pageNumObj instanceof Number n) {
            pNum = n.intValue();
        } else if (pageNumObj instanceof String s) {
            try {
                pNum = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                pNum = -1;
            }
        }
        this.pageNumber = pNum;

        this.sectionTitle = (String) metadata.getOrDefault("sectionTitle", "");

        float cScore = 1.0f;
        Object scoreObj = metadata.get("confidenceScore");
        if (scoreObj instanceof Number n) {
            cScore = n.floatValue();
        } else if (scoreObj instanceof String s) {
            try {
                cScore = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                cScore = 1.0f;
            }
        }
        this.confidenceScore = cScore;
    }

    public Citation(int index, String content, String sourceUri, String title, int pageNumber,
            String sectionTitle, float confidenceScore) {
        this.index = index;
        this.content = content;
        this.sourceUri = sourceUri;
        this.title = title;
        this.pageNumber = pageNumber;
        this.sectionTitle = sectionTitle;
        this.confidenceScore = confidenceScore;
    }

    public int getIndex() {
        return index;
    }

    public String getContent() {
        return content;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public String getTitle() {
        return title;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public float getConfidenceScore() {
        return confidenceScore;
    }
}
