package no.ssb.forbruk.nets.scheduling;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @NonNull
    private final NetsHandle netsHandle;
    @NonNull
    private final MeterRegistry meterRegistry;

    private AtomicInteger numberOfFilesDbCounted = new AtomicInteger(0);
    private AtomicInteger numberOfTransactionsDbCounted = new AtomicInteger(0);

    private static boolean okToRun = true;

    @Timed( value = "forbruk_nets_app_scheduledtask", description = "Time spent running scheduled task")
    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void handleNetsTransactions() {
        if (okToRun) {
            try {
                netsHandle.initialize(numberOfFilesDbCounted, numberOfTransactionsDbCounted);
                logger.info("Called netsfiles - " + LocalDateTime.now());
                netsHandle.getAndHandleNetsFiles();
            } catch (Exception e) {
                okToRun = false;
                meterRegistry.counter("forbruk_nets_app_error_scheduledtask", "error", "store handlenetstransactions");
                logger.error("Something went wrong in initializing netsHandle or handling files: {}", e.getMessage());
                e.printStackTrace();
            }
        }
        netsHandle.endHandleNetsFiles();
    }

    @Scheduled(cron = "${scheduled.cron.cleantable}")
    public void cleantable() {
        try {
            netsHandle.deleteAllFromDBTable();
            logger.info("Deleted all from db-table forbruk_nets_files - " + LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Something went wrong in deleting tablerows{}", e.getMessage());
            e.printStackTrace();
        }
    }

}
