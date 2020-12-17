package no.ssb.forbruk.nets.scheduling;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final NetsHandle netsHandle;
    private final MeterRegistry meterRegistry;

    private static boolean okToRun = true;

    @Timed( value = "forbruk_nets_app_scheduledtask", description = "Time spent running scheduled task")
    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void handleNetsTransactions() {
        if (okToRun) {
            try {
                netsHandle.initialize();
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
}
