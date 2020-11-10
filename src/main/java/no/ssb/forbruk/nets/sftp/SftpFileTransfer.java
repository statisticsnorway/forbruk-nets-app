package no.ssb.forbruk.nets.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.ssb.forbruk.nets.avro.AvroConverter;
import no.ssb.forbruk.nets.model.NetsRecord;
import no.ssb.forbruk.nets.repository.NetsRecordRepository;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    NetsRecordRepository netsRecordRepository;

    private AvroConverter avroConverter;

    private static ChannelSftp channelSftp;
    private Session jschSession;

    public void list() {
        try {
            setupJsch();
            avroConverter = new AvroConverter("netsTransaction.avsc");
            logger.info("workdir: {}", WORKDIR);
            saveFileRecord("list files in " + WORKDIR);
            listDirectories(WORKDIR);
            listFilesInPath(WORKDIR);
//            saveFilesInPath(WORKDIR);
        } catch (IOException e) {
            logger.error("IO-feil: {}", e.toString());
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        } catch (JSchException e) {
            logger.error("jsch-feil: {}", e.toString());
        }
        disconnectJsch();
    }


    void saveFilesInPath(String path) throws SftpException {
        fileList(path).forEach(this::saveFile);
    }

    void listDirectories(String path) throws SftpException {
        listDirectory(path);
    }

    void listFilesInPath(String path) throws SftpException {
        fileList(path).forEach(f -> logger.info("file in {}: {}", path, f.getFilename()));
    }

    static void listDirectory(String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
        logger.info("List files in {}", path);
        for (ChannelSftp.LsEntry entry : files) {
            logger.info("entry: {}", entry.toString());
            if (!entry.getAttrs().isDir()) {
                logger.info("path: {}", path);
                logger.info("file: {}/{}" , path, entry.getFilename());
            } else {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    listDirectory(path + "/" + entry.getFilename());
                }
            }
        }
    }


    private Collection<ChannelSftp.LsEntry> fileList(String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
        Collection<ChannelSftp.LsEntry> fileList = Collections.list(files.elements());
        return fileList;
    }


    private void saveFile(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getFilename());
        try {
//            channelSftp.get(WORKDIR + "/" + f.getFilename()), fileDir + f.getFilename());
            InputStream fileStream = channelSftp.get(WORKDIR + "/" + f.getFilename());
            List<GenericRecord> records;
            try {
                records = avroConverter.convertCsvToAvro(fileStream, ";");
                logger.info("Converted to {}", records);
            } catch (IOException e) {
                logger.error("Error in reading filestream for {}: {}", f.getFilename(), e.getMessage());
            }

            saveFileRecord(f.getLongname());
        } catch (SftpException e) {
            logger.error("Error in saving file {}: {}", f.getFilename(), e.getMessage());
        }
    }

    private void saveFileRecord(String content) {
        logger.info("file in path: {}", content);
        NetsRecord nr = new NetsRecord();
        nr.setContent(content);
        nr.setTimestamp(LocalDateTime.now());
        NetsRecord saved = netsRecordRepository.save(nr);
    }


    private void convertToAvro() {
        AvroConverter avroConverter = new AvroConverter();

    }




    private void setupJsch() throws JSchException, IOException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");

        String tmpPrivateKeyFile = "tmp/" + "nets" + ".pk";
        logger.info("privateKeyFile: {}", tmpPrivateKeyFile);
        Files.write(Path.of(tmpPrivateKeyFile),
                    privatekeyfile.isEmpty() ?
                    privateKey.getBytes() : Files.readAllBytes(Path.of(privatekeyfile)));
        logger.info("privatekey: {}", Files.readString(Path.of(tmpPrivateKeyFile)).substring(0,70));
        logger.info("privatekey: {}", StringUtils.substring(Files.readString(Path.of(tmpPrivateKeyFile)),-50));
        jsch.addIdentity(tmpPrivateKeyFile, passphrase);
        logger.info("get jschSession");
        jschSession = jsch.getSession(USER, HOST, PORT);
        logger.info("set StrictHostKeyChecking to no");
        jschSession.setConfig("StrictHostKeyChecking", "no");
        logger.info("connect with session timeout {}", SESSION_TIMEOUT);
        jschSession.connect(SESSION_TIMEOUT);
        logger.info("jsession connected ? {}", jschSession.isConnected());

        logger.info("open channel with type sftp");
        channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        logger.info("connect with channel timeout {}", CHANNEL_TIMEOUT);
        channelSftp.connect(CHANNEL_TIMEOUT);
        logger.info("delete tmp file if exists ({})", Files.exists(Path.of(tmpPrivateKeyFile)));
        Files.deleteIfExists(Path.of(tmpPrivateKeyFile));
        logger.info("Connected?: {}", channelSftp.isConnected());
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
