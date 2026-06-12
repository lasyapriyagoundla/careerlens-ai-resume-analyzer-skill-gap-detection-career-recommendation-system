package chennu.com.studentexceptionapi.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResumeAnalysisResult {

    private String fileName;
    private String extractedText;
    private String analysisSource;
    private List<SkillScore> detectedSkills = new ArrayList<>();
    private Map<String, List<String>> categorizedSkills = new LinkedHashMap<>();
    private List<AreaScore> strongAreas = new ArrayList<>();
    private List<AreaScore> improvementAreas = new ArrayList<>();
    private List<SkillGapItem> skillGap = new ArrayList<>();
    private List<RoleRecommendation> recommendedRoles = new ArrayList<>();
    private Map<String, Integer> evaluationScores = new LinkedHashMap<>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getAnalysisSource() {
        return analysisSource;
    }

    public void setAnalysisSource(String analysisSource) {
        this.analysisSource = analysisSource;
    }

    public List<SkillScore> getDetectedSkills() {
        return detectedSkills;
    }

    public void setDetectedSkills(List<SkillScore> detectedSkills) {
        this.detectedSkills = detectedSkills;
    }

    public Map<String, List<String>> getCategorizedSkills() {
        return categorizedSkills;
    }

    public void setCategorizedSkills(Map<String, List<String>> categorizedSkills) {
        this.categorizedSkills = categorizedSkills;
    }

    public List<AreaScore> getStrongAreas() {
        return strongAreas;
    }

    public void setStrongAreas(List<AreaScore> strongAreas) {
        this.strongAreas = strongAreas;
    }

    public List<AreaScore> getImprovementAreas() {
        return improvementAreas;
    }

    public void setImprovementAreas(List<AreaScore> improvementAreas) {
        this.improvementAreas = improvementAreas;
    }

    public List<SkillGapItem> getSkillGap() {
        return skillGap;
    }

    public void setSkillGap(List<SkillGapItem> skillGap) {
        this.skillGap = skillGap;
    }

    public List<RoleRecommendation> getRecommendedRoles() {
        return recommendedRoles;
    }

    public void setRecommendedRoles(List<RoleRecommendation> recommendedRoles) {
        this.recommendedRoles = recommendedRoles;
    }

    public Map<String, Integer> getEvaluationScores() {
        return evaluationScores;
    }

    public void setEvaluationScores(Map<String, Integer> evaluationScores) {
        this.evaluationScores = evaluationScores;
    }

    public static class SkillScore {
        private String skill;
        private int score;
        private String level;

        public SkillScore() {
        }

        public SkillScore(String skill, int score, String level) {
            this.skill = skill;
            this.score = score;
            this.level = level;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }
    }

    public static class AreaScore {
        private String area;
        private int score;

        public AreaScore() {
        }

        public AreaScore(String area, int score) {
            this.area = area;
            this.score = score;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    public static class SkillGapItem {
        private String skill;
        private String status;

        public SkillGapItem() {
        }

        public SkillGapItem(String skill, String status) {
            this.skill = skill;
            this.status = status;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class RoleRecommendation {
        private String role;
        private int match;
        private String note;

        public RoleRecommendation() {
        }

        public RoleRecommendation(String role, int match, String note) {
            this.role = role;
            this.match = match;
            this.note = note;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public int getMatch() {
            return match;
        }

        public void setMatch(int match) {
            this.match = match;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
