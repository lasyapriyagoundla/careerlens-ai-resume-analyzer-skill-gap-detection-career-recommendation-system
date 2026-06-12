package chennu.com.studentexceptionapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CareerlensBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerlensBackendApplication.class, args);
	}
}
