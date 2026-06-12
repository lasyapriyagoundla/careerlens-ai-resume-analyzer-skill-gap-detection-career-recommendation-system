package chennu.com.studentexceptionapi.service;

import chennu.com.studentexceptionapi.model.InternshipFeedResponse;
import chennu.com.studentexceptionapi.model.InternshipPosting;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class InternshipFeedService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final AtomicReference<List<InternshipPosting>> cache = new AtomicReference<>(new ArrayList<>());
    private volatile Instant lastUpdated = Instant.EPOCH;
    private volatile List<String> lastSources = List.of();

    @Value("${internship.feed.limit:60}")
    private int feedLimit;

    @Value("${internship.feed.remotive.url:https://remotive.com/api/remote-jobs?search=intern}")
    private String remotiveUrl;

    @Value("${internship.feed.arbeitnow.url:https://www.arbeitnow.com/api/job-board-api}")
    private String arbeitnowUrl;

    @Value("${internship.feed.unstop.url:}")
    private String unstopOfficialUrl;

    @Value("${internship.feed.unstop.api.key:}")
    private String unstopOfficialApiKey;

    public InternshipFeedService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    @PostConstruct
    public void bootstrap() {
        refreshFeed();
    }

    @Scheduled(fixedDelayString = "${internship.feed.refresh.ms:1800000}")
    public void scheduledRefresh() {
        refreshFeed();
    }

    public synchronized void refreshFeed() {
        List<InternshipPosting> merged = new ArrayList<>();
        Set<String> sources = new LinkedHashSet<>();

        merged.addAll(fetchRemotive());
        if (!merged.isEmpty()) {
            sources.add("Remotive");
        }

        List<InternshipPosting> arbeitnow = fetchArbeitnow();
        merged.addAll(arbeitnow);
        if (!arbeitnow.isEmpty()) {
            sources.add("Arbeitnow");
        }

        List<InternshipPosting> unstopOfficial = fetchUnstopOfficial();
        merged.addAll(unstopOfficial);
        if (!unstopOfficial.isEmpty()) {
            sources.add("Unstop Official API");
        }

        merged = dedupeAndSort(merged);
        if (merged.size() > feedLimit) {
            merged = merged.subList(0, feedLimit);
        }

        if (!merged.isEmpty()) {
            cache.set(merged);
            lastUpdated = Instant.now();
            lastSources = new ArrayList<>(sources);
        }
    }

    public InternshipFeedResponse getFeed() {
        List<InternshipPosting> internships = cache.get();
        long ageSeconds = lastUpdated.equals(Instant.EPOCH)
                ? -1
                : Duration.between(lastUpdated, Instant.now()).getSeconds();

        return new InternshipFeedResponse(
                internships,
                lastUpdated.equals(Instant.EPOCH) ? null : DateTimeFormatter.ISO_INSTANT.format(lastUpdated),
                ageSeconds,
                lastSources);
    }

    private List<InternshipPosting> fetchRemotive() {
        try {
            JsonNode root = readJson(remotiveUrl);
            JsonNode jobs = root.path("jobs");
            if (!jobs.isArray()) {
                return List.of();
            }

            List<InternshipPosting> postings = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isInternshipTitle(title)) {
                    continue;
                }

                postings.add(new InternshipPosting(
                        "Remotive",
                        title,
                        job.path("company_name").asText("Unknown Company"),
                        normalizeLocation(job.path("candidate_required_location").asText("Remote")),
                        job.path("url").asText(""),
                        job.path("publication_date").asText(""),
                        sanitizeSalary(job.path("salary").asText("As per company standards")),
                        jsonArrayToStrings(job.path("tags"))));
            }
            return postings;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<InternshipPosting> fetchArbeitnow() {
        try {
            JsonNode root = readJson(arbeitnowUrl);
            JsonNode jobs = root.path("data");
            if (!jobs.isArray()) {
                return List.of();
            }

            List<InternshipPosting> postings = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = job.path("title").asText("");
                if (!isInternshipTitle(title)) {
                    continue;
                }

                postings.add(new InternshipPosting(
                        "Arbeitnow",
                        title,
                        job.path("company_name").asText("Unknown Company"),
                        normalizeLocation(job.path("location").asText("Remote")),
                        job.path("url").asText(""),
                        job.path("created_at").asText(""),
                        "As per company standards",
                        jsonArrayToStrings(job.path("tags"))));
            }
            return postings;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<InternshipPosting> fetchUnstopOfficial() {
        if (unstopOfficialUrl == null || unstopOfficialUrl.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = readJsonWithOptionalApiKey(unstopOfficialUrl, unstopOfficialApiKey);
            JsonNode jobs = extractJobsNode(root);
            if (!jobs.isArray()) {
                return List.of();
            }

            List<InternshipPosting> postings = new ArrayList<>();
            for (JsonNode job : jobs) {
                String title = readText(job, "title", "jobTitle", "name");
                if (!isInternshipTitle(title)) {
                    continue;
                }

                postings.add(new InternshipPosting(
                        "Unstop Official API",
                        title,
                        readText(job, "company", "companyName", "organization"),
                        normalizeLocation(readText(job, "location", "city", "workLocation")),
                        readText(job, "applyUrl", "url", "link"),
                        readText(job, "postedAt", "createdAt", "publishDate"),
                        sanitizeSalary(readText(job, "salary", "stipend", "compensation")),
                        jsonArrayToStrings(job.path("tags"))));
            }

            return postings;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private JsonNode readJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Internship feed source returned status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private JsonNode readJsonWithOptionalApiKey(String url, String apiKey) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("Accept", "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
            builder.header("X-API-Key", apiKey);
        }

        HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Internship feed source returned status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private JsonNode extractJobsNode(JsonNode root) {
        if (root.path("data").isArray()) {
            return root.path("data");
        }
        if (root.path("jobs").isArray()) {
            return root.path("jobs");
        }
        if (root.path("items").isArray()) {
            return root.path("items");
        }
        return root;
    }

    private String readText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = node.path(key).asText("").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private boolean isInternshipTitle(String title) {
        String normalized = title.toLowerCase(Locale.ENGLISH);
        return normalized.contains("intern")
                || normalized.contains("internship")
                || normalized.contains("trainee")
                || normalized.contains("apprentice")
                || normalized.contains("co-op")
                || normalized.contains("co op");
    }

    private List<InternshipPosting> dedupeAndSort(List<InternshipPosting> list) {
        Set<String> seen = new LinkedHashSet<>();
        List<InternshipPosting> unique = new ArrayList<>();

        for (InternshipPosting posting : list) {
            String key = (posting.getCompany() + "|" + posting.getTitle() + "|" + posting.getApplyUrl())
                    .toLowerCase(Locale.ENGLISH);
            if (seen.add(key)) {
                unique.add(posting);
            }
        }

        unique.sort(Comparator.comparing(InternshipPosting::getPostedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return unique;
    }

    private List<String> jsonArrayToStrings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            String text = value.asText("").trim();
            if (!text.isEmpty()) {
                values.add(text);
            }
        }
        return values;
    }

    private String sanitizeSalary(String value) {
        if (value == null || value.isBlank()) {
            return "As per company standards";
        }
        return value;
    }

    private String normalizeLocation(String value) {
        if (value == null || value.isBlank()) {
            return "Remote";
        }
        return value;
    }
}
