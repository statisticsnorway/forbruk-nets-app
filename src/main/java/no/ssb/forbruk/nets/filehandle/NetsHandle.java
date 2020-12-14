package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.db.model.service.ForbrukNetsLogService;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

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
    final ForbrukNetsLogService forbrukNetsLogService;

    @NonNull
    final MeterRegistry meterRegistry;

    public void initialize() throws IOException, JSchException {
        sftpFileTransfer.setupJsch();
        googleCloudStorage.setupGoogleCloudStorage();
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
            forbrukNetsLogService.saveLogError("-","forbruk_nets_app_error_handlenetsfiles", 1L);
        }
    }

    public void endHandleNetsFiles() {
        sftpFileTransfer.disconnectJsch();
    }

    private void handleFile(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getFilename());
        try {
            // get file from nets
            InputStream inputStream = sftpFileTransfer.getFileInputStream(f);
            // store filecontent to gcs
            googleCloudStorage.produceMessages(inputStream, f.getFilename());

            meterRegistry.counter("forbruk_nets_app_handle_files", "count", "filestreated").increment();
            forbrukNetsLogService.saveLogOK(f.getFilename(), "file handled", 1L);
        } catch (SftpException e) {
            meterRegistry.counter("forbruk_nets_app_error_handle_file_sftp", "error", "getfileinputstream").increment();
            logger.error("Error in saving/reading file {}: {}", f.getFilename(), e.getMessage());
            forbrukNetsLogService.saveLogError("-","forbruk_nets_app_error_handle_file_sftp", 1L);
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_handle_file", "error", "handle_file").increment();
            logger.error("Error producing messages for {}: {}", f.getFilename(), e.getMessage());
            forbrukNetsLogService.saveLogError("-","forbruk_nets_app_error_handle_file", 1L);
        }
    }

}
