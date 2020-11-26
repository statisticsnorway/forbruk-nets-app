package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import no.ssb.forbruk.nets.db.model.NetsRecord;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
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

    public void getAndHandleNetsFiles() {
        try {
            sftpFileTransfer.setupJsch();
            googleCloudStorage.initialize(headerLine);

            logger.info("find files and loop");
            /* handle files in path */
            sftpFileTransfer.fileList().forEach(this::handleFile);
        } catch (IOException e) {
            logger.error("IO-feil: {}", e.toString());
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        } catch (JSchException e) {
            logger.error("jsch-feil: {}", e.toString());
        }
        sftpFileTransfer.disconnectJsch();
        printDb();
    }

    private void handleFile(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getFilename());
        try {
            InputStream inputStream = sftpFileTransfer.getFileInputStream(f);
            googleCloudStorage.produceMessages(inputStream, f.getFilename());

            saveFileRecord(f.getFilename());

            logger.info("read from bucket");
            googleCloudStorage.consumeMessages();
            logger.info("finished handled file");
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
        dbrecs.forEach(d -> logger.info(d.toString()));
    }


}
