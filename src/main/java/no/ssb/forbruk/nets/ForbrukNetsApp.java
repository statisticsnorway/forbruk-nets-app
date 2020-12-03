package no.ssb.forbruk.nets;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.RunMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ForbrukNetsApp implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ForbrukNetsApp.class);

	@Autowired
	RunMain runMain;

	public static void main(String[] args) {
		SpringApplication.run(ForbrukNetsApp.class, args);
		logger.info("Startup complete.");
	}

	@Bean
	public TimedAspect timedAspect(MeterRegistry registry) {
		return new TimedAspect(registry);
	}

	@Override
	public void run(String... args) throws Exception {
		runMain.run();
		Thread.sleep(60000);
		System.exit(0);
	}
}
