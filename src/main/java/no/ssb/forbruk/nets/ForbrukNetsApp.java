package no.ssb.forbruk.nets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ForbrukNetsApp {

	private static final Logger logger = LoggerFactory.getLogger(ForbrukNetsApp.class);

	public static void main(String[] args) {
		SpringApplication.run(ForbrukNetsApp.class, args);
		logger.info("Startup complete.");
	}

}
