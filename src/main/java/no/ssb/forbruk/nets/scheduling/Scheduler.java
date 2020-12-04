package no.ssb.forbruk.nets.scheduling;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class Scheduler {
    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    NetsHandle netsHandle;

    @Autowired
    MeterRegistry meterRegistry;

    @Timed( value = "forbruk_nets_app_scheduledtask", description = "Time spent running scheduled task")
    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void handleNetsTransactions() {
        try {
            netsHandle.initialize();
            logger.info("Called netsfiles - " + LocalDateTime.now());
            netsHandle.getAndHandleNetsFiles();
            netsHandle.endHandleNetsFiles();
        } catch (IOException e) {
            logger.info("Something went wrong in initializing netsHandle: {}", e.getMessage());
            e.printStackTrace();
        } catch (JSchException e) {
            logger.info("Something went wrong in initializing Jsch: {}", e.getMessage());
            e.printStackTrace();
        }

    }
}
