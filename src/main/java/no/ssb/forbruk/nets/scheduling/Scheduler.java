package no.ssb.forbruk.nets.scheduling;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private final NetsHandle netsHandle;
    private final MeterRegistry meterRegistry;

    @Timed( value = "forbruk_nets_app_scheduledtask", description = "Time spent running scheduled task")
    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void handleNetsTransactions() {
        try {
            netsHandle.initialize();
            logger.info("Called netsfiles - " + LocalDateTime.now());
            netsHandle.getAndHandleNetsFiles();
        } catch (IOException e) {
            meterRegistry.counter("forbruk_nets_app_error_scheduledtask_io", "error", "store handlenetstransactions");
            logger.info("Something went wrong in initializing netsHandle or handling files: {}", e.getMessage());
            e.printStackTrace();
        } catch (JSchException e) {
            meterRegistry.counter("forbruk_nets_app_error_scheduledtask", "error", "store handlenetstransactions");
            logger.info("Something went wrong in initializing Jsch: {}", e.getMessage());
            e.printStackTrace();
        }
        netsHandle.endHandleNetsFiles();

    }
}
