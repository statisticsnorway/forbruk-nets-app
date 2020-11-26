package no.ssb.forbruk.nets.filehandle.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.ssb.forbruk.nets.db.model.NetsRecord;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;


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


    private static ChannelSftp channelSftp;
    private Session jschSession;


    public Collection<ChannelSftp.LsEntry> fileList() throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(WORKDIR);
        Collection<ChannelSftp.LsEntry> fileList = Collections.list(files.elements());
        return fileList;
    }

    public InputStream getFileInputStream(ChannelSftp.LsEntry f) throws SftpException {
        return channelSftp.get(WORKDIR + "/" + f.getFilename());
    }

    public void setupJsch() throws JSchException, IOException {
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


    public void disconnectJsch() {
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
