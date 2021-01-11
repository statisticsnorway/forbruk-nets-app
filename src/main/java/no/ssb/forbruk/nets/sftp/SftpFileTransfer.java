package no.ssb.forbruk.nets.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequiredArgsConstructor
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

    @Value("#{environment.NETS_PASSPHRASE}")
    private String passphrase;
    @Value("#{environment.NETS_SECRET}")
    private String privateKey;


    private static ChannelSftp channelSftp = new ChannelSftp();
    private Session jschSession;

    @NonNull
    private MeterRegistry meterRegistry;

    @Timed(value="forbruk_nets_app_filelist", description = "Time get one list of files from nets")
    public Collection<ChannelSftp.LsEntry> fileList() throws SftpException {
        // get and return list of all files in <WORKDIR> at nets
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(WORKDIR);
        logger.info("number of files in {}: {}", WORKDIR, files.size());
        Collection<ChannelSftp.LsEntry> fileList = Collections.list(files.elements());
        meterRegistry.counter("forbruk_nets_app.files", "filesFromNets", "count").increment(fileList.size());
        fileList.forEach(f -> logger.info(f.getFilename()));
        return fileList;
    }

    @Timed(value = "forbruk_nets_app_readonefile", description = "Time get one file from nets")
    public InputStream getFileInputStream(ChannelSftp.LsEntry f) throws SftpException {
        // get given file as inputstream from nets
        return channelSftp.get(WORKDIR + "/" + f.getFilename());
    }

    public boolean setupJsch() throws JSchException, IOException {
        // create jsch-session and open channel - and connect to nets
        logger.info("nets user {}, host {} and port {} - passphrase-length: {}, ({})", USER, HOST, PORT, passphrase.length(), passphrase.substring(3,5));
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");

        String tmpPrivateKeyFile = createTemporaryPrivateKeyFile();
        String pk = new String(Files.readAllBytes(Path.of(tmpPrivateKeyFile)));
        logger.info("pk: {} ... {}", pk.substring(0,100), pk.substring(pk.length()-50));

        jsch.addIdentity(tmpPrivateKeyFile, passphrase);
        jschSession = jsch.getSession(USER, HOST, PORT);
        jschSession.setConfig("StrictHostKeyChecking", "no");
        jschSession.connect(SESSION_TIMEOUT);

        channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
        channelSftp.connect(CHANNEL_TIMEOUT);
        // delete tmp-keyfile if exists"
        Files.deleteIfExists(Path.of(tmpPrivateKeyFile));
        return channelSftp.isConnected();
    }


    private String createTemporaryPrivateKeyFile() throws IOException {
        // create private-key-file from environment-given secret
        logger.info("privatekeyfile: {}", privatekeyfile.isEmpty() ? " null" : privatekeyfile);
        logger.info("privateKey: {}", privateKey == null ? " null" : privateKey.substring(privateKey.length()-50));
        String tmpPrivateKeyFile = "tmp/" + "nets" + ".pk";
        Files.write(Path.of(tmpPrivateKeyFile),
                    privatekeyfile.isEmpty() ?
                    privateKey.getBytes() : Files.readAllBytes(Path.of(privatekeyfile)));
        return tmpPrivateKeyFile;
    }


    public void disconnectJsch() {
        //disconnect from nets
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
