package no.ssb.forbruk.nets.sftp;

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
    private static String HOST;
    @Value("${forbruk.nets.user}")
    private static String USER;
    @Value("${forbruk.nets.port}")
    private static int PORT;
    @Value("${forbruk.nets.session_timeout}")
    private static int SESSION_TIMEOUT;
    @Value("${forbruk.nets.channel_timeout}")
    private static int CHANNEL_TIMEOUT;
    @Value("${forbruk.nets.workdir}")
    private static String WORKDIR;

    @Autowired
    AppSecretsService appSecretsService;

    @Autowired
    NetsRecordRepository netsRecordRepository;

    private static Session jschSession;

    public void list() {
        try {
            listDirectories(WORKDIR);
            listFilesInPath(WORKDIR);
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        } catch (JSchException e) {
            logger.info("jsch-feil: {}", e.toString());
        }
    }

    void listFilesInPath(String path) throws SftpException, JSchException {
        fileList(path).forEach(this::saveFileRecord);
    }

    void listDirectories(String path) throws SftpException, JSchException {

        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect(CHANNEL_TIMEOUT);
        listDirectory(channelSftp, path);

        channelSftp.disconnect();
        jschSession.disconnect();
    }

    static void listDirectory(ChannelSftp channelSftp, String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
        for (ChannelSftp.LsEntry entry : files) {
            logger.info("entry: {}", entry.toString());
            if (!entry.getAttrs().isDir()) {
                logger.info("path: {}", path);
                logger.info("file: {}/{}" , path, entry.getFilename());
            } else {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    listDirectory(channelSftp, path + "/" + entry.getFilename());
                }
            }
        }
    }


    Collection<ChannelSftp.LsEntry> fileList(String path) throws JSchException, SftpException {
        ChannelSftp channelSftp = setupJsch();
        channelSftp.connect(CHANNEL_TIMEOUT);

        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
        Collection<ChannelSftp.LsEntry> fileList = Collections.list(files.elements());
        channelSftp.disconnect();
        jschSession.disconnect();
        return fileList;
    }

    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");
        String privateKey = appSecretsService.getNetsSecret();
        String passphrase = appSecretsService.getNetsPassphrase();
        jsch.addIdentity(privateKey, passphrase);
        jschSession = jsch.getSession(USER, HOST, PORT);
        jschSession.setConfig("StrictHostKeyChecking", "no");
        jschSession.connect(SESSION_TIMEOUT);
        return (ChannelSftp) jschSession.openChannel("sftp");
    }


    private void saveFileRecord(ChannelSftp.LsEntry f) {
        logger.info("file in path: {}", f.getLongname());
        NetsRecord nr = new NetsRecord();
        nr.setContent(f.getLongname());
        NetsRecord saved = netsRecordRepository.save(nr);
    }
}
