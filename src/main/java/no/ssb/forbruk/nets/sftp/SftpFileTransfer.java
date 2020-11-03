package no.ssb.forbruk.nets.sftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.ssb.forbruk.nets.model.NetsRecord;
import no.ssb.forbruk.nets.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.service.AppSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;


@Service
@Transactional
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

    @Value("${forbruk.nets.privatekey}")
    private String privateKey;
    @Value("${forbruk.nets.passphrase}")
    private String passphrase;

    @Value("${forbruk.nets.filedir}")
    private String fileDir;

    @Autowired
    AppSecretsService appSecretsService;

    @Autowired
    NetsRecordRepository netsRecordRepository;

    private static ChannelSftp channelSftp;
    private Session jschSession;

    public void list() {
        try {
            setupJsch();
            logger.info("workdir: {}", WORKDIR);
            listDirectories(WORKDIR);
            listFilesInPath(WORKDIR);
//            saveFilesInPath(WORKDIR);
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
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(fileStream));
                String line;
                while ((line = br.readLine()) != null) {
                    logger.info(line);
                }
            } catch (IOException io) {
                logger.error("IOException occurred during handling file from SFTP server due to {}", io.getMessage());
            } catch (Exception e) {
                logger.error("Exception occurred during handling file from SFTP server due to {}", e.getMessage());
            }

            saveFileRecord(f);
        } catch (SftpException e) {
            logger.error("Error in saving file {}: {}", f.getFilename(), e.getMessage());
        }
    }

    private void saveFileRecord(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getLongname());
        NetsRecord nr = new NetsRecord();
        nr.setContent(f.getLongname());
        nr.setTimestamp(LocalDateTime.now());
        NetsRecord saved = netsRecordRepository.save(nr);
    }




    private void setupJsch() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");
        privateKey = privateKey != null && !privateKey.isEmpty() ? privateKey : appSecretsService.getNetsSecret();
        passphrase = passphrase != null && !passphrase.isEmpty() ? passphrase : appSecretsService.getNetsPassphrase();
        jsch.addIdentity(privateKey, passphrase);
        jschSession = jsch.getSession(USER, HOST, PORT);
        jschSession.setConfig("StrictHostKeyChecking", "no");
        jschSession.connect(SESSION_TIMEOUT);
        channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        channelSftp.connect(CHANNEL_TIMEOUT);
    }


    private void disconnectJsch() {
        channelSftp.disconnect();
        jschSession.disconnect();
    }
}
