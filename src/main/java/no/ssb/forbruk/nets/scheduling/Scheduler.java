package no.ssb.forbruk.nets.scheduling;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import no.ssb.forbruk.nets.metrics.MetricsManager;
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
    MetricsManager metricsManager;

    @Timed(description = "Time spent running scheduled task")
    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void handleNetsTransactions() {
        logMainMetrics();
        try {
            netsHandle.initialize(metricsManager);
            logger.info("Called netsfiles - " + LocalDateTime.now());
//            netsHandle.getAndHandleNetsFiles();
            netsHandle.endHandleNetsFiles();
        } catch (IOException e) {
            logger.info("Something went wrong in initializing netsHandle: {}", e.getMessage());
            e.printStackTrace();
        } catch (JSchException e) {
            logger.info("Something went wrong in initializing Jsch: {}", e.getMessage());
            e.printStackTrace();
        }

    }

    private void logMainMetrics() {
        logger.info("Call tracker - " + LocalDateTime.now());
        //TODO: lean how gauge-metrics works
        metricsManager.registerGauge("forbruk_nets_app.reg", Double.valueOf("1"));
        metricsManager.trackGaugeMetrics("forbruk_nets_app.called", 1.0d, "controllerCalled", "run");
        metricsManager.trackCounterMetrics("forbruk_nets_app.total", 1, "totalMethodCall", "run");
    }

}
