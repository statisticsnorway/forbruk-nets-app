package no.ssb.forbruk.nets.controller;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class NetsController {
    private static final Logger logger = LoggerFactory.getLogger(NetsController.class);

    @Autowired
    NetsHandle netsHandle;

    @Autowired
    private MetricsManager metricsManager;

    @GetMapping("/netsfiles")
    @Timed(description = "Time spent running controller")
    public ResponseEntity<String> handleNetsFiles() {
        logMainMetrics();

        try {
            this.netsHandle.initialize(metricsManager);
            logger.info("Called netsfiles - " + LocalDateTime.now());
            netsHandle.getAndHandleNetsFiles();
            netsHandle.endHandleNetsFiles();
        } catch (IOException e) {
            logger.info("Something went wrong in initializing netsHandle: {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Noe gikk dessverre feil.", HttpStatus.FAILED_DEPENDENCY);
        } catch (JSchException e) {
            logger.info("Something went wrong in initializing Jsch: {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Noe gikk dessverre feil i kommunikasjon med Nets.", HttpStatus.FAILED_DEPENDENCY);
        }

        return new ResponseEntity<>("Files treated", HttpStatus.OK);

    }

    private void logMainMetrics() {
        logger.info("Call tracker - " + LocalDateTime.now());
        metricsManager.registerGauge("forbruk_nets_app.reg", Double.valueOf("1"));
        metricsManager.trackGaugeMetrics("forbruk_nets_app.called", 1.0d, "controllerCalled", "run");
        metricsManager.trackCounterMetrics("forbruk_nets_app.total", 1, "totalMethodCall", "run");
    }
}

