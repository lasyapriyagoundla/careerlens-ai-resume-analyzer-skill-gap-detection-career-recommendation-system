package chennu.com.studentexceptionapi.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult;
import chennu.com.studentexceptionapi.model.Resume;
import chennu.com.studentexceptionapi.model.ResumeEntity;
import chennu.com.studentexceptionapi.repository.ResumeRepository;

@Service
public class ResumeService {

    @Autowired
    private ResumeRepository repository;

    @Autowired
    private ResumeTextExtractor textExtractor;

    @Autowired
    private ResumeAnalyzer resumeAnalyzer;

    public ResponseEntity<ResumeEntity<Resume>> saveResume(Resume resume) {
        Resume saved = repository.save(resume);
        return ResponseEntity.ok(ResumeEntity.ok(saved));
    }

    public ResponseEntity<ResumeEntity<List<Resume>>> getAllResumes() {
        List<Resume> resumes = repository.findAll();
        return ResponseEntity.ok(ResumeEntity.ok(resumes));
    }

    public ResponseEntity<ResumeEntity<Resume>> getResumeById(Long id) {
        Resume resume = repository.findById(id).orElse(null);
        if (resume == null) {
            return ResponseEntity.ok(ResumeEntity.notFound());
        }
        return ResponseEntity.ok(ResumeEntity.ok(resume));
    }

    public ResponseEntity<ResumeEntity<Resume>> updateResume(Long id, Resume resume) {
        if (repository.existsById(id)) {
            resume.setId(id);
            Resume updated = repository.save(resume);
            return ResponseEntity.ok(ResumeEntity.ok(updated));
        }
        return ResponseEntity.ok(ResumeEntity.notFound());
    }

    public ResponseEntity<ResumeEntity<String>> deleteResume(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok(ResumeEntity.ok("Resume deleted successfully!"));
        }
        return ResponseEntity.ok(ResumeEntity.notFound());
    }

    public ResponseEntity<ResumeEntity<ResumeAnalysisResult>> uploadAndAnalyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResumeEntity.error("Please upload a resume file."));
        }

        if (!textExtractor.isSupported(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResumeEntity.error("Unsupported file type. Allowed: pdf, doc, docx, txt, rtf, md, csv."));
        }

        try {
            String extractedText = textExtractor.extractText(file);
            if (extractedText == null || extractedText.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResumeEntity.error("Could not extract any text from the uploaded file."));
            }

            ResumeAnalysisResult result = resumeAnalyzer.analyze(file.getOriginalFilename(), extractedText);
            result.setExtractedText(extractedText);
            return ResponseEntity.ok(ResumeEntity.ok(result));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResumeEntity.error("Resume analysis failed: " + ex.getMessage()));
        }
    }
}