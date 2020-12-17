package no.ssb.forbruk.nets.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class NetsController {
    private static final Logger logger = LoggerFactory.getLogger(NetsController.class);

    NetsHandle netsHandle;
    MeterRegistry meterRegistry;

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
        } catch (Exception e) {
            logger.error("Something went wrong in handling netsfiles {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong in handling netsfiles ", HttpStatus.EXPECTATION_FAILED);
        }
    }


}

