package chennu.com.studentexceptionapi.controller;

import chennu.com.studentexceptionapi.model.InternshipFeedResponse;
import chennu.com.studentexceptionapi.model.ResumeEntity;
import chennu.com.studentexceptionapi.service.InternshipFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internships")
@CrossOrigin(origins = "*")
public class InternshipController {

    private final InternshipFeedService internshipFeedService;

    public InternshipController(InternshipFeedService internshipFeedService) {
        this.internshipFeedService = internshipFeedService;
    }

    @GetMapping("/live")
    public ResponseEntity<ResumeEntity<InternshipFeedResponse>> getLiveInternships() {
        InternshipFeedResponse response = internshipFeedService.getFeed();
        return ResponseEntity.ok(ResumeEntity.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResumeEntity<InternshipFeedResponse>> refreshNow() {
        internshipFeedService.refreshFeed();
        InternshipFeedResponse response = internshipFeedService.getFeed();
        return ResponseEntity.ok(ResumeEntity.ok(response));
    }
}
