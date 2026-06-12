package chennu.com.studentexceptionapi.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import chennu.com.studentexceptionapi.model.ResumeAnalysisResult;
import chennu.com.studentexceptionapi.model.Resume;
import chennu.com.studentexceptionapi.model.ResumeEntity;
import chennu.com.studentexceptionapi.service.ResumeService;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "*")
public class ResumeController {

    @Autowired
    private ResumeService service;

    @PostMapping
    public ResponseEntity<ResumeEntity<Resume>> createResume(@RequestBody Resume resume) {
        return service.saveResume(resume);
    }

    @PostMapping(value = "/upload-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeEntity<ResumeAnalysisResult>> uploadAndAnalyze(@RequestParam("file") MultipartFile file) {
        return service.uploadAndAnalyze(file);
    }

    @GetMapping
    public ResponseEntity<ResumeEntity<List<Resume>>> getAllResumes() {
        return service.getAllResumes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeEntity<Resume>> getResumeById(@PathVariable Long id) {
        return service.getResumeById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeEntity<Resume>> updateResume(@PathVariable Long id, @RequestBody Resume resume) {
        return service.updateResume(id, resume);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResumeEntity<String>> deleteResume(@PathVariable Long id) {
        return service.deleteResume(id);
    }

    @GetMapping("/test")
    public String test() {
        return "Resume API is working!";
    }
}