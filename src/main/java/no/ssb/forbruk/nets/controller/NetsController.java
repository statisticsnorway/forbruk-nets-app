package no.ssb.forbruk.nets.controller;

import com.jcraft.jsch.JSchException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Timer;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import no.ssb.forbruk.nets.filehandle.RunMain;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetsController {

    @Autowired
    RunMain runMain;

    @Autowired
    MetricsManager metricsManager;

    @GetMapping("/netsfiles")
    public ResponseEntity<String> handleNetsFiles() {
        runMain.runNetsHandle(metricsManager);
        return new ResponseEntity<>("Files treated", HttpStatus.OK);

    }


}

