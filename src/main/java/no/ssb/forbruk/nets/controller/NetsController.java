package no.ssb.forbruk.nets.controller;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
public class NetsController {
    private static final Logger logger = LoggerFactory.getLogger(NetsController.class);

    NetsHandle netsHandle;
    MeterRegistry metricsRegistry;

    public NetsController (NetsHandle netsHandle) {
        this.netsHandle = netsHandle;
    }

    @GetMapping("/netsfiles")
    @Timed(value="forbruk_nets_app_controller", description = "Time spent running controller")
    public ResponseEntity<String> handleNetsFiles() {
        try {
            netsHandle.initialize();
            logger.info("Called netsfiles - " + LocalDateTime.now());
            netsHandle.getAndHandleNetsFiles();
            netsHandle.endHandleNetsFiles();
            return new ResponseEntity<>("Files treated", HttpStatus.OK);
        } catch (IOException e) {
            logger.info("Something went wrong in initializing netsHandle: {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong in initializing netsHandle", HttpStatus.EXPECTATION_FAILED);
        } catch (JSchException e) {
            logger.info("Something went wrong in initializing Jsch: {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong in initializing Jsch", HttpStatus.EXPECTATION_FAILED);
        }
    }


}

