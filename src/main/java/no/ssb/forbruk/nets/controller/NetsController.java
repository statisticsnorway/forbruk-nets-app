package no.ssb.forbruk.nets.controller;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsFilesRepository;
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
    private final ForbrukNetsFilesRepository forbrukNetsFilesRepository;


    public NetsController (NetsHandle netsHandle,
                           ForbrukNetsFilesRepository forbrukNetsFilesRepository,
                           MeterRegistry meterRegistry) {
        this.netsHandle = netsHandle;
        this.forbrukNetsFilesRepository = forbrukNetsFilesRepository;
        this.meterRegistry = meterRegistry;
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

    @GetMapping("/fileshandled")
    public ResponseEntity<String> countHandledNetsFiles() {
        try {
            long numberOfHandledFiles = forbrukNetsFilesRepository.count();
            int numberOfStoredTransactions = forbrukNetsFilesRepository.findAll().stream()
                    .mapToInt(x -> x.getTransactions().intValue())
                    .sum();
            meterRegistry.gauge("forbruk_nets_app_db_files", numberOfHandledFiles);
            meterRegistry.gauge("forbruk_nets_app_db_transactions", numberOfStoredTransactions);

            return new ResponseEntity<>("a total of " + numberOfHandledFiles + " files and " +
                    numberOfStoredTransactions + " transactions are handled and stored", HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Something went wrong in getting database information {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong in getting database information ", HttpStatus.EXPECTATION_FAILED);
        }
    }

}

