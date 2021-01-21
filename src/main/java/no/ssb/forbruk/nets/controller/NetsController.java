package no.ssb.forbruk.nets.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.db.model.ForbrukNetsFiles;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsFilesRepository;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class NetsController {
    private static final Logger logger = LoggerFactory.getLogger(NetsController.class);

    NetsHandle netsHandle;
    MeterRegistry meterRegistry;
    private final ForbrukNetsFilesRepository forbrukNetsFilesRepository;

    private AtomicInteger numberOfFilesDbCounted;
    private AtomicInteger numberOfTransactionsDbCounted;

    public NetsController (NetsHandle netsHandle,
                           ForbrukNetsFilesRepository forbrukNetsFilesRepository,
                           MeterRegistry meterRegistry) {
        this.netsHandle = netsHandle;
        this.forbrukNetsFilesRepository = forbrukNetsFilesRepository;
        this.meterRegistry = meterRegistry;
        numberOfFilesDbCounted = this.meterRegistry.gauge("forbruk_nets_app_files_db_count", new AtomicInteger(0));
        numberOfTransactionsDbCounted = this.meterRegistry.gauge("forbruk_nets_app_transactions_db_count", new AtomicInteger(0));
    }

//    @GetMapping("/netsfiles")
//    @Timed(value="forbruk_nets_app_controller", description = "Time spent running controller")
//    public ResponseEntity<String> handleNetsFiles() {
//        try {
//            netsHandle.initialize();
//            logger.info("Called netsfiles - " + LocalDateTime.now());
//            netsHandle.getAndHandleNetsFiles();
//            netsHandle.endHandleNetsFiles();
//            return new ResponseEntity<>("Files treated", HttpStatus.OK);
//        } catch (Exception e) {
//            logger.error("Something went wrong in handling netsfiles {}", e.getMessage());
//            e.printStackTrace();
//            return new ResponseEntity<>("Something went wrong in handling netsfiles ", HttpStatus.EXPECTATION_FAILED);
//        }
//    }

    @GetMapping("/fileshandled")
    public ResponseEntity<String> countHandledNetsFiles() {
        try {
            long numberOfHandledFiles = forbrukNetsFilesRepository.count();

            List<ForbrukNetsFiles> filesAndTransactions = forbrukNetsFilesRepository.findAll();
            String filesAndTransactionsJson = createJsonFromResultset(filesAndTransactions);

            int numberOfStoredTransactions = filesAndTransactions.stream()
                    .mapToInt(x -> x.getTransactions().intValue())
                    .sum();

            numberOfFilesDbCounted.set( (int) numberOfHandledFiles);
            numberOfTransactionsDbCounted.set(numberOfStoredTransactions);

            return new ResponseEntity<>("a total of " + numberOfHandledFiles + " files and " +
                    numberOfStoredTransactions + " transactions are handled and stored \n" + filesAndTransactionsJson , HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Something went wrong in getting database information {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong in getting database information ", HttpStatus.EXPECTATION_FAILED);
        }
    }

    @GetMapping("/listfilesatnets")
    public ResponseEntity<String> listFilesAtNets() {
        try {
            netsHandle.initialize(numberOfFilesDbCounted, numberOfTransactionsDbCounted);
            logger.info("Called netsfiles - " + LocalDateTime.now());
            String fileJson = netsHandle.listAllNetsFiles();
            netsHandle.endHandleNetsFiles();
            return new ResponseEntity<>(fileJson, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Something went wrong listing nets-files {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong listing nets-files ", HttpStatus.EXPECTATION_FAILED);
        }
    }

    private String createJsonFromResultset(List<ForbrukNetsFiles> filesAndTransactions) {
        Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().setDateFormat("dd.MM.yyyy hh.mm.ss").create();
        return gsonBuilder.toJson(filesAndTransactions);

    }

}

