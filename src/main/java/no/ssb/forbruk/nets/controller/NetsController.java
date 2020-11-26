package no.ssb.forbruk.nets.controller;

import no.ssb.forbruk.nets.filehandle.NetsHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class NetsController {
    private static final Logger logger = LoggerFactory.getLogger(NetsController.class);

    @Autowired
    NetsHandle netsHandle;


    @GetMapping("/netsfiles")
    public ResponseEntity<String> runSftpFileTransferList() {
        logger.info("Called netsfiles - " + LocalDateTime.now());
        netsHandle.getAndHandleNetsFiles();
        return new ResponseEntity<>("Files treated", HttpStatus.OK);

    }
}

