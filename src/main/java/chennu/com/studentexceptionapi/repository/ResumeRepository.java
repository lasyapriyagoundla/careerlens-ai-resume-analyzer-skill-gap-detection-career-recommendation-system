package chennu.com.studentexceptionapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import chennu.com.studentexceptionapi.model.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
}