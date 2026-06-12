package chennu.com.studentexceptionapi.service;

import chennu.com.studentexceptionapi.model.ResumeAnalysisResult;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult.AreaScore;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult.RoleRecommendation;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult.SkillGapItem;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult.SkillScore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalyzer {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepSeekApiUrl;

    @Value("${deepseek.api.key:}")
    private String deepSeekApiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    public ResumeAnalyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    }

    public ResumeAnalysisResult analyze(String fileName, String text) {
        ResumeAnalysisResult aiResult = tryDeepSeekAnalysis(fileName, text);
        if (aiResult != null) {
            ensureMinimumData(aiResult, fileName, text, "DeepSeek");
            return aiResult;
        }

        ResumeAnalysisResult fallback = buildHeuristicAnalysis(fileName, text);
        fallback.setAnalysisSource("Rule-Based");
        return fallback;
    }

    private ResumeAnalysisResult tryDeepSeekAnalysis(String fileName, String text) {
        if (deepSeekApiKey == null || deepSeekApiKey.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", deepSeekModel);
            requestBody.put("temperature", 0.2);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system",
                "content", "You are a resume analyzer. Return strict JSON only without markdown fences."
            ));
            messages.add(Map.of(
                "role", "user",
                "content", buildPrompt(text)
            ));
            requestBody.put("messages", messages);

            String payload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deepSeekApiUrl))
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                return null;
            }

            String cleanJson = unwrapMarkdownJson(content);
            ResumeAnalysisResult result = objectMapper.readValue(cleanJson, ResumeAnalysisResult.class);
            result.setFileName(fileName);
            return result;
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildPrompt(String text) {
        String safeText = text.length() > 14000 ? text.substring(0, 14000) : text;
        return "Analyze this resume text and return JSON with exact keys: "
            + "detectedSkills (array of {skill,score,level}), "
            + "categorizedSkills (object with categories as arrays), "
            + "strongAreas (array of {area,score}), "
            + "improvementAreas (array of {area,score}), "
            + "skillGap (array of {skill,status}), "
            + "recommendedRoles (array of {role,match,note}), "
            + "evaluationScores (object with integers 0-100 for skillRelevance, certificationScore, resumeMatch). "
            + "Use 5-10 entries in detectedSkills and real software/data/ML categories where applicable. "
            + "Resume text:\n" + safeText;
    }

    private String unwrapMarkdownJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            if (firstNewLine > 0) {
                trimmed = trimmed.substring(firstNewLine + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private ResumeAnalysisResult buildHeuristicAnalysis(String fileName, String text) {
        ResumeAnalysisResult result = new ResumeAnalysisResult();
        result.setFileName(fileName);
        result.setExtractedText(text);

        Map<String, List<String>> categoryKeywords = new LinkedHashMap<>();
        categoryKeywords.put("Programming", Arrays.asList("java", "python", "javascript", "typescript", "c++", "c#", "go", "sql"));
        categoryKeywords.put("Machine Learning", Arrays.asList("machine learning", "deep learning", "tensorflow", "pytorch", "scikit-learn", "nlp"));
        categoryKeywords.put("Data", Arrays.asList("pandas", "numpy", "power bi", "tableau", "data analysis", "statistics"));
        categoryKeywords.put("Backend", Arrays.asList("spring", "spring boot", "node", "django", "flask", "rest", "api"));
        categoryKeywords.put("Cloud", Arrays.asList("aws", "azure", "gcp", "docker", "kubernetes"));

        String lower = text.toLowerCase(Locale.ENGLISH);
        Map<String, List<String>> categorized = new LinkedHashMap<>();
        Set<String> allSkills = new LinkedHashSet<>();

        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            List<String> found = new ArrayList<>();
            for (String skill : entry.getValue()) {
                if (lower.contains(skill.toLowerCase(Locale.ENGLISH))) {
                    String normalized = normalizeSkill(skill);
                    found.add(normalized);
                    allSkills.add(normalized);
                }
            }
            categorized.put(entry.getKey(), found);
        }

        List<SkillScore> detected = new ArrayList<>();
        for (String skill : allSkills) {
            int score = estimateScore(lower, skill);
            detected.add(new SkillScore(skill, score, scoreToLevel(score)));
        }

        List<AreaScore> strong = new ArrayList<>();
        List<AreaScore> improve = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : categorized.entrySet()) {
            int catScore = entry.getValue().isEmpty() ? 30 : Math.min(95, 52 + (entry.getValue().size() * 11));
            if (catScore >= 65) {
                strong.add(new AreaScore(entry.getKey(), catScore));
            } else {
                improve.add(new AreaScore(entry.getKey(), catScore));
            }
        }

        List<SkillGapItem> gaps = new ArrayList<>();
        gaps.add(new SkillGapItem("Deep Learning", containsAny(lower, "deep learning", "tensorflow", "pytorch") ? "Sufficient" : "Needs Improvement"));
        gaps.add(new SkillGapItem("Natural Language Processing", containsAny(lower, "nlp", "natural language processing", "transformers") ? "Sufficient" : "Missing"));
        gaps.add(new SkillGapItem("Cloud", containsAny(lower, "aws", "azure", "gcp") ? "Needs Improvement" : "Missing"));
        gaps.add(new SkillGapItem("Feature Engineering", containsAny(lower, "feature engineering", "data preprocessing") ? "Sufficient" : "Needs Improvement"));

        List<RoleRecommendation> roles = new ArrayList<>();
        roles.add(new RoleRecommendation("Machine Learning Engineer", roleScore(lower, "machine learning", "tensorflow", "pytorch", "python"), "Builds and deploys ML solutions."));
        roles.add(new RoleRecommendation("Data Scientist", roleScore(lower, "python", "statistics", "pandas", "data analysis"), "Finds insights from large data sets."));
        roles.add(new RoleRecommendation("AI Specialist", roleScore(lower, "nlp", "deep learning", "model", "ai"), "Designs AI-powered applications."));

        Map<String, Integer> evaluation = new LinkedHashMap<>();
        evaluation.put("skillRelevance", averageScore(strong));
        evaluation.put("certificationScore", containsAny(lower, "certification", "aws certified", "google data analytics") ? 78 : 62);
        evaluation.put("resumeMatch", Math.min(95, 58 + (detected.size() * 4)));

        result.setDetectedSkills(detected);
        result.setCategorizedSkills(categorized);
        result.setStrongAreas(strong);
        result.setImprovementAreas(improve);
        result.setSkillGap(gaps);
        result.setRecommendedRoles(roles);
        result.setEvaluationScores(evaluation);
        return result;
    }

    private void ensureMinimumData(ResumeAnalysisResult result, String fileName, String text, String source) {
        result.setFileName(fileName);
        result.setExtractedText(text);
        result.setAnalysisSource(source);

        if (result.getDetectedSkills() == null) {
            result.setDetectedSkills(new ArrayList<>());
        }
        if (result.getCategorizedSkills() == null) {
            result.setCategorizedSkills(new LinkedHashMap<>());
        }
        if (result.getStrongAreas() == null) {
            result.setStrongAreas(new ArrayList<>());
        }
        if (result.getImprovementAreas() == null) {
            result.setImprovementAreas(new ArrayList<>());
        }
        if (result.getSkillGap() == null) {
            result.setSkillGap(new ArrayList<>());
        }
        if (result.getRecommendedRoles() == null) {
            result.setRecommendedRoles(new ArrayList<>());
        }
        if (result.getEvaluationScores() == null) {
            result.setEvaluationScores(new LinkedHashMap<>());
        }

        if (result.getEvaluationScores().isEmpty()) {
            result.getEvaluationScores().put("skillRelevance", 75);
            result.getEvaluationScores().put("certificationScore", 72);
            result.getEvaluationScores().put("resumeMatch", 78);
        }
    }

    private int estimateScore(String text, String skill) {
        int base = 55;
        int occurrences = countOccurrences(text, skill.toLowerCase(Locale.ENGLISH));
        return Math.min(95, base + (occurrences * 10));
    }

    private int countOccurrences(String text, String key) {
        int idx = 0;
        int count = 0;
        while ((idx = text.indexOf(key, idx)) != -1) {
            count++;
            idx += key.length();
        }
        return count;
    }

    private String scoreToLevel(int score) {
        if (score >= 85) {
            return "Advanced";
        }
        if (score >= 70) {
            return "Intermediate";
        }
        return "Basic";
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    private int roleScore(String text, String... terms) {
        int matched = 0;
        for (String term : terms) {
            if (containsAny(text, term)) {
                matched++;
            }
        }
        return Math.min(96, 55 + matched * 11);
    }

    private int averageScore(List<AreaScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return 68;
        }
        int total = 0;
        for (AreaScore score : scores) {
            total += score.getScore();
        }
        return total / scores.size();
    }

    private String normalizeSkill(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String value = raw.trim();
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ENGLISH);
        }
        String first = value.substring(0, 1).toUpperCase(Locale.ENGLISH);
        String rest = value.substring(1).toLowerCase(Locale.ENGLISH);
        return first + rest;
    }
}
