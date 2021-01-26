package no.ssb.forbruk.nets.filehandle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.db.model.ForbrukNetsFiles;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsFilesRepository;
import no.ssb.forbruk.nets.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Timed
public class NetsHandle {
    private static final Logger logger = LoggerFactory.getLogger(NetsHandle.class);

    @NonNull
    private final SftpFileTransfer sftpFileTransfer;

    @NonNull
    private final GoogleCloudStorage googleCloudStorage;

    @NonNull
    private final MeterRegistry meterRegistry;

    @NonNull
    private final ForbrukNetsFilesRepository forbrukNetsFilesRepository;

    private AtomicInteger numberOfFilesDb;
    private AtomicInteger numberOfTransactionsDb;

    public void initialize( AtomicInteger numberOfFilesDb, AtomicInteger numberOfTransactionsDb) throws IOException, JSchException {
        sftpFileTransfer.setupJsch();
        googleCloudStorage.setupGoogleCloudStorage();
        this.numberOfFilesDb = numberOfFilesDb;
        this.numberOfTransactionsDb = numberOfTransactionsDb;

    }

    @Timed(value = "forbruk_nets_app_handlenetsfiles", description = "Time handling files from nets", longTask = true)
    public void getAndHandleNetsFiles() throws Exception {
        try {
            logger.info("find files and loop");
            /* handle files in path - for-loop to exit on exception*/
            List<String> handledForbrukNetsFiles = forbrukNetsFilesRepository.findAll()
                    .stream()
                    .map(ForbrukNetsFiles::getFilename)
                    .collect(Collectors.toList());
            handledForbrukNetsFiles.forEach( f-> logger.info("File handled earlier: {}", f));
            List<ChannelSftp.LsEntry> newNetsFiles = sftpFileTransfer.fileList()
                    .stream()
                    .filter(f -> !handledForbrukNetsFiles.contains(f.getFilename()))
                    .collect(Collectors.toList());

            logger.info("Number of files to be handled now: {}", newNetsFiles.size());
            newNetsFiles.forEach( f -> logger.info("File to be handled now: {} ({})", f.getFilename(), f.getAttrs().getSize()));


            for (ChannelSftp.LsEntry file : newNetsFiles) {
                handleFile(file);
            }
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_handlenetsfiles","error", "sftp").increment();
            logger.error("Sftp-feil: {}", e.toString());
            throw new Exception();
        }

    }

    public void endHandleNetsFiles() {
        sftpFileTransfer.disconnectJsch();
    }

    private void handleFile(ChannelSftp.LsEntry f) throws Exception {
        logger.info("file in path: {}", f.getFilename());
        try {
            // get file from nets
            InputStream inputStream = sftpFileTransfer.getFileInputStream(f);
            // store filecontent to gcs
            int antTransactions = googleCloudStorage.produceMessages(inputStream, f.getFilename());

            logSuccessedFile(f.getFilename(), antTransactions);
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_handle_file", "file", f.getFilename(), "error", "handle_file").increment();
            logger.error("Error producing messages for {}: {} \n {}", f.getFilename(), e.getMessage(), e.getStackTrace());
            throw new Exception(e);
        }
    }

    private void logSuccessedFile(String filename, int antTransactions) {
        meterRegistry.counter("forbruk_nets_app_handle_files", "file", filename, "count", "filestreated").increment();
        forbrukNetsFilesRepository.save(ForbrukNetsFiles.builder()
                .filename(filename)
                .transactions((long)antTransactions)
                .timestamp(LocalDateTime.now())
                .build());

        long numberOfHandledFiles = forbrukNetsFilesRepository.count();
        int numberOfStoredTransactions = forbrukNetsFilesRepository.findAll().stream()
                .mapToInt(x -> x.getTransactions().intValue())
                .sum();

        numberOfFilesDb.set( (int) numberOfHandledFiles);
        numberOfTransactionsDb.set(numberOfStoredTransactions);

    }

    public String listAllNetsFiles() throws SftpException {
        List<ChannelSftp.LsEntry> netsFiles = sftpFileTransfer.fileList()
                .stream()
                .collect(Collectors.toList());

        logger.info("{} files at Nets:", netsFiles.size());
        netsFiles.forEach( f -> logger.info("File : {} ({})", f.getFilename(), f.getAttrs().getSize()));
        return new GsonBuilder().setPrettyPrinting().create().toJson(netsFiles);
    }


    public void deleteAllFromDBTable() throws Exception {
        forbrukNetsFilesRepository.deleteAll();
        logger.info("Deleted all from db-table forbruk_nets_files - " + LocalDateTime.now());
    }

}
