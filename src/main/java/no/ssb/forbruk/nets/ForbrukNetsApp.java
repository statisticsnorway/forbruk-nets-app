package no.ssb.forbruk.nets;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import no.ssb.forbruk.nets.filehandle.RunMain;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootApplication
public class ForbrukNetsApp  implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(ForbrukNetsApp.class);

	@Autowired
	private MetricsManager metricsManager;

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
		runMain.runNetsHandle(metricsManager);
//		System.exit(0);
	}
}
