package chennu.com.studentexceptionapi.model;

import java.util.List;

public class InternshipPosting {
    private String source;
    private String title;
    private String company;
    private String location;
    private String applyUrl;
    private String postedAt;
    private String salaryText;
    private List<String> tags;

    public InternshipPosting() {
    }

    public InternshipPosting(String source, String title, String company, String location, String applyUrl, String postedAt,
            String salaryText, List<String> tags) {
        this.source = source;
        this.title = title;
        this.company = company;
        this.location = location;
        this.applyUrl = applyUrl;
        this.postedAt = postedAt;
        this.salaryText = salaryText;
        this.tags = tags;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getApplyUrl() {
        return applyUrl;
    }

    public void setApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
    }

    public String getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(String postedAt) {
        this.postedAt = postedAt;
    }

    public String getSalaryText() {
        return salaryText;
    }

    public void setSalaryText(String salaryText) {
        this.salaryText = salaryText;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
