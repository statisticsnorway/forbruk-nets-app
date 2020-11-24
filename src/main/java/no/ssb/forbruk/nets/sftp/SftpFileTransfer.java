package no.ssb.forbruk.nets.sftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.ssb.forbruk.nets.db.model.NetsRecord;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class SftpFileTransfer {
    private static final Logger logger = LoggerFactory.getLogger(SftpFileTransfer.class);


    @Value("${forbruk.nets.host}")
    private String HOST;
    @Value("${forbruk.nets.user}")
    private String USER;
    @Value("${forbruk.nets.port}")
    private int PORT;
    @Value("${forbruk.nets.session_timeout}")
    private int SESSION_TIMEOUT;
    @Value("${forbruk.nets.channel_timeout}")
    private int CHANNEL_TIMEOUT;
    @Value("${forbruk.nets.workdir}")
    private String WORKDIR;
    @Value("${forbruk.nets.privatekeyfile}")
    private String privatekeyfile;


    @Value("#{environment.NETS_PASSPHRASE_STAGING}")
    private String passphrase;
    @Value("#{environment.NETS_SECRET_STAGING}")
    private String privateKey;

    @Value("${forbruk.nets.filedir}")
    private String fileDir;
    @Value("${forbruk.nets.header}")
    private String headerLine;

    @Autowired
    NetsRecordRepository netsRecordRepository;

    @Autowired
    private GoogleCloudStorage googleCloudStorage;

    private static ChannelSftp channelSftp;
    private Session jschSession;


    public void getAndHandleNetsFiles() {
        try {
            setupJsch();
            googleCloudStorage.initialize(headerLine);

            logger.info("find files and loop");
            /* handle files in path */
            fileList(WORKDIR).forEach(this::handleFile);
        } catch (IOException e) {
            logger.error("IO-feil: {}", e.toString());
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        } catch (JSchException e) {
            logger.error("jsch-feil: {}", e.toString());
        }
        disconnectJsch();
        printDb();
    }


    private Collection<ChannelSftp.LsEntry> fileList(String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
        Collection<ChannelSftp.LsEntry> fileList = Collections.list(files.elements());
        return fileList;
    }


    private void handleFile(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getFilename());
        try {
            channelSftp.get(WORKDIR + "/" + f.getFilename(), fileDir + f.getFilename());
            googleCloudStorage.produceMessages(fileDir + f.getFilename());

            saveFileRecord(f.getFilename());
            logger.info("read from bucket");
            googleCloudStorage.readFromBucket();
            logger.info("finished handled file");
        } catch (SftpException e) {
            logger.error("Error in saving/reading file {}: {}", f.getFilename(), e.getMessage());
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
        List<NetsRecord>  dbrecs = netsRecordRepository.findAll();
        dbrecs.forEach(d -> logger.info(d.toString()));
    }


    private void setupJsch() throws JSchException, IOException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");

        String tmpPrivateKeyFile = createTemporaryPrivateKeyFile();
        jsch.addIdentity(tmpPrivateKeyFile, passphrase);
        jschSession = jsch.getSession(USER, HOST, PORT);
        jschSession.setConfig("StrictHostKeyChecking", "no");
        jschSession.connect(SESSION_TIMEOUT);

        channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        channelSftp.connect(CHANNEL_TIMEOUT);
//        logger.info("delete tmp file if exists ({})", Files.exists(Path.of(tmpPrivateKeyFile)));
        Files.deleteIfExists(Path.of(tmpPrivateKeyFile));
        logger.info("Connected?: {}", channelSftp.isConnected());
    }


    private String createTemporaryPrivateKeyFile() throws IOException {
        String tmpPrivateKeyFile = "tmp/" + "nets" + ".pk";
        Files.write(Path.of(tmpPrivateKeyFile),
                    privatekeyfile.isEmpty() ?
                    privateKey.getBytes() : Files.readAllBytes(Path.of(privatekeyfile)));
        return tmpPrivateKeyFile;
    }


    private void disconnectJsch() {
        if (channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if (jschSession.isConnected()) {
            jschSession.disconnect();
        }
    }

    public boolean secretsOk() {
        return passphrase != null && !passphrase.isEmpty() && privateKey != null && !privateKey.isEmpty();
    }

}
