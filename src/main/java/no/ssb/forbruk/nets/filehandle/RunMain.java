package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class RunMain {
    private static final Logger logger = LoggerFactory.getLogger(RunMain.class);

    @Autowired
    NetsHandle netsHandle;


    @Timed(description = "Time spent running controller")
    public void runNetsHandle (MetricsManager metricsManager) {
        logMainMetrics(metricsManager);
        try {
            netsHandle.initialize(metricsManager);
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

    private void logMainMetrics(MetricsManager metricsManager) {
        logger.info("Call tracker - " + LocalDateTime.now());
        metricsManager.registerGauge("forbruk_nets_app.reg", Double.valueOf("1"));
        metricsManager.trackGaugeMetrics("forbruk_nets_app.called", 1.0d, "controllerCalled", "run");
        metricsManager.trackCounterMetrics("forbruk_nets_app.total", 1, "totalMethodCall", "run");
    }

}
