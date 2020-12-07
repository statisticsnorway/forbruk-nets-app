package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.annotation.Timed;
import no.ssb.forbruk.nets.db.model.NetsRecord;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Timed
public class NetsHandle {
    private static final Logger logger = LoggerFactory.getLogger(NetsHandle.class);

    @Autowired
    SftpFileTransfer sftpFileTransfer;

    @Autowired
    GoogleCloudStorage googleCloudStorage;

    @Autowired
    NetsRecordRepository netsRecordRepository;

    @Value("${forbruk.nets.header}")
    String headerLine;

    MetricsManager metricsManager;

    public void initialize(MetricsManager metricsManager) throws IOException, JSchException {
        this.metricsManager = metricsManager;
        this.sftpFileTransfer.initialize(metricsManager);
        logger.info("sftpFileTransfer initialized");
        this.googleCloudStorage.initialize(headerLine, metricsManager);
        logger.info("googleCloudStorage initialized");
    }

    @Timed(description = "Time handling files from nets", longTask = true)
    public void getAndHandleNetsFiles() {
        try {
            logger.info("find files and loop");
            /* handle files in path */
            sftpFileTransfer.fileList().forEach(this::handleFile);
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        }
    }

    public void endHandleNetsFiles() {
        sftpFileTransfer.disconnectJsch();
        printDb();
    }

    private void handleFile(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getFilename());
        try {
            InputStream inputStream = sftpFileTransfer.getFileInputStream(f);
            int totalTransactions = googleCloudStorage.produceMessages(inputStream, f.getFilename());

            saveFileRecord(totalTransactions + " transactions saved for " + f.getFilename());

            logger.info("read from bucket");
            googleCloudStorage.consumeMessages();
            logger.info("finished handled file");
            metricsManager.trackCounterMetrics("forbruk_nets_app.file", 1, "filesTreated", "count");
        } catch (SftpException e) {
            logger.error("Error in saving/reading file {}: {}", f.getFilename(), e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Error producing messages for {}: {}", f.getFilename(), e.getMessage());
            e.printStackTrace();
        }
    }


    private void saveFileRecord(String content) {
        logger.info("file in path: {}", content);
        NetsRecord nr = new NetsRecord();
        nr.setContent(content);
        nr.setTimestamp(LocalDateTime.now());
        NetsRecord saved = netsRecordRepository.save(nr);
    }

    private void printDb() {
        List<NetsRecord> dbrecs = netsRecordRepository.findAll();
        logger.info("antall rader i base: {}", dbrecs.size());
//        dbrecs.forEach(d -> logger.info(d.toString()));
        }


}
