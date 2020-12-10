package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.ssb.forbruk.nets.db.model.NetsRecord;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Timed
public class NetsHandle {
    private static final Logger logger = LoggerFactory.getLogger(NetsHandle.class);

    @NonNull
    final SftpFileTransfer sftpFileTransfer;

    @NonNull
    final GoogleCloudStorage googleCloudStorage;

    @NonNull
    final NetsRecordRepository netsRecordRepository;

    @NonNull
    final MeterRegistry meterRegistry;

    public void initialize() throws IOException, JSchException {
        sftpFileTransfer.setupJsch();
        logger.info("sftpFileTransfer initialized");
        googleCloudStorage.initialize();
        logger.info("googleCloudStorage initialized");
    }

    @Timed(value = "forbruk_nets_app_handlenetsfiles", description = "Time handling files from nets", longTask = true)
    public void getAndHandleNetsFiles() {
        try {
            logger.info("find files and loop");
            /* handle files in path */
            sftpFileTransfer.fileList().forEach(this::handleFile);
        } catch (SftpException e) {
            meterRegistry.counter("forbruk_nets_app_error_handlenetsfiles","error", "sftp").increment();
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
            meterRegistry.counter("forbruk_nets_app_handle_files", "count", "filestreated").increment();
        } catch (SftpException e) {
            meterRegistry.counter("forbruk_nets_app_error_handle_file_sftp", "error", "getfileinputstream");
            logger.error("Error in saving/reading file {}: {}", f.getFilename(), e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_handle_file", "error", "handle_file");
            logger.error("Error producing messages for {}: {}", f.getFilename(), e.getMessage());
            e.printStackTrace();
        }
    }


    private void saveFileRecord(String content) {
        logger.info("file in path: {}", content);
        netsRecordRepository.save(NetsRecord.builder()
                .content(content)
                .timestamp(LocalDateTime.now())
                .build());
    }

    private void printDb() { // TODO: Remove this method
        List<NetsRecord> dbrecs = netsRecordRepository.findAll();
        logger.info("antall rader i base: {}", dbrecs.size());
//        dbrecs.forEach(d -> logger.info(d.toString()));
        }


}
