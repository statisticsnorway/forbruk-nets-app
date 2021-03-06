package no.ssb.forbruk.nets;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ForbrukNetsApp { // implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ForbrukNetsApp.class);

	public static void main(String[] args) {
		SpringApplication.run(ForbrukNetsApp.class, args);
//		System.exit(SpringApplication.exit(SpringApplication.run(ForbrukNetsApp.class, args)));
		logger.info("Startup complete.");
	}

	@Bean
	public TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry);
	}

}
