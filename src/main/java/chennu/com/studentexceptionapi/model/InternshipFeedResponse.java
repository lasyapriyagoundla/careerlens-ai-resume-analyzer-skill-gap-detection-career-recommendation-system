package chennu.com.studentexceptionapi.model;

import java.util.List;

public class InternshipFeedResponse {
    private List<InternshipPosting> internships;
    private String lastUpdated;
    private long cacheAgeSeconds;
    private List<String> sources;

    public InternshipFeedResponse() {
    }

    public InternshipFeedResponse(List<InternshipPosting> internships, String lastUpdated, long cacheAgeSeconds,
            List<String> sources) {
        this.internships = internships;
        this.lastUpdated = lastUpdated;
        this.cacheAgeSeconds = cacheAgeSeconds;
        this.sources = sources;
    }

    public List<InternshipPosting> getInternships() {
        return internships;
    }

    public void setInternships(List<InternshipPosting> internships) {
        this.internships = internships;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getCacheAgeSeconds() {
        return cacheAgeSeconds;
    }

    public void setCacheAgeSeconds(long cacheAgeSeconds) {
        this.cacheAgeSeconds = cacheAgeSeconds;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }
}
