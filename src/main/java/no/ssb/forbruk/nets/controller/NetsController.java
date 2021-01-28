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

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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

            int numberOfStoredTransactions = filesAndTransactions.stream()
                    .mapToInt(x -> x.getTransactions().intValue())
                    .sum();
            String filesAndTransactionsOutput = createOutputFromResultset(filesAndTransactions, numberOfHandledFiles, numberOfStoredTransactions);

            numberOfFilesDbCounted.set( (int) numberOfHandledFiles);
            numberOfTransactionsDbCounted.set(numberOfStoredTransactions);

            return new ResponseEntity<>( filesAndTransactionsOutput, HttpStatus.OK);
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


    @GetMapping("/deleteidrow")
    public ResponseEntity<String> deleteRowWithId() {
        try {
            Long id = 32L;
            Optional<ForbrukNetsFiles> idRow = forbrukNetsFilesRepository.findById(id);
            logger.info("row with id {} : {}", id, idRow);

            forbrukNetsFilesRepository.deleteById(id);

            logger.info("Deleted row with id {}", id);

            Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("dd.MM.yyyy hh.mm.ss").create();
            return new ResponseEntity<>(gson.toJson(idRow), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Something went wrong listing nets-files {}", e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Something went wrong listing nets-files ", HttpStatus.EXPECTATION_FAILED);
        }
    }

    private String createOutputFromResultset(List<ForbrukNetsFiles> filesAndTransactions, long numberOfHandledFiles, int numberOfStoredTransactions) {
        StringBuilder sb = new StringBuilder("<html><body><table>");
        sb.append("<thead><tr><th colspan=\"4\">A total of ").append(numberOfHandledFiles).append(" files and ").append(numberOfStoredTransactions)
        .append(" transactions is handled and stored.").append("</th></tr></thead>");
        filesAndTransactions.forEach(f -> {
            sb.append("<tbody><tr>")
            .append("<td>").append(f.getId()).append("</td>")
            .append("<td>").append(f.getFilename()).append("</td>")
            .append("<td>").append(f.getTransactions()).append("</td>")
            .append("<td>").append(new SimpleDateFormat("dd.MM.yyyy HH.mm.ss").format(f.getTimestamp())).append("</td>")
            .append("</tr></tbody>");
        });
        sb.append("</table></body></html>");
        return sb.toString();
    }
}

