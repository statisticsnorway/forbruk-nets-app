package no.ssb.forbruk.nets.sftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.forbruk.nets.avro.AvroConverter;
import no.ssb.forbruk.nets.model.NetsRecord;
import no.ssb.forbruk.nets.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import org.apache.avro.generic.GenericRecord;
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

    @Value("${google.storage.provider.bucket}")
    private String storageBucket;
    @Value("${forbruk.nets.filedir}")
    private String fileDir;
    @Value("${forbruk.env}")
    private String runenv;

    @Autowired
    NetsRecordRepository netsRecordRepository;

    private AvroConverter avroConverter;
    private GoogleCloudStorage googleCloudStorage;
    private String storageLocation;

    private static ChannelSftp channelSftp;
    private Session jschSession;


    public void getAndHandleNetsFiles() {
        try {
            setupJsch();
            avroConverter = new AvroConverter("netsTransaction.avsc");
            storageLocation = "local".equals(runenv) ? fileDir : "gs://" + storageBucket + "/";
            googleCloudStorage = new GoogleCloudStorage("tmp/rawdata/nets", storageLocation, privatekeyfile, "test-rawdata-stream");
            logger.info("storagebucket: {}", storageBucket);

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
            /** Test 1: Get netsfile and save it to filedir immediately */
            channelSftp.get(WORKDIR + "/" + f.getFilename(), fileDir + f.getFilename());
            googleCloudStorage.storeToBucket(fileDir + f.getFilename());
//            Files.readAllLines(Path.of(fileDir + f.getFilename())).forEach(l -> logger.info("fillinje: {}", l));

//            /** Test 2: Get netsfile as inputstream, convert it to avroRecords and save these */
//            ByteArrayInputStream fileStream = new ByteArrayInputStream(channelSftp.get(WORKDIR + "/" + f.getFilename()).readAllBytes());
////            InputStream fileStream = new FileInputStream(new File(fileDir + f.getFilename()));
////            InputStream fileStream = getClass().getClassLoader().getResourceAsStream("testNetsResponse.csv");
////            ByteArrayInputStream fileStream = new ByteArrayInputStream(getClass().getClassLoader().getResourceAsStream("testNetsResponse.csv").readAllBytes());
//            List<GenericRecord> records = new ArrayList<>();
//            try {
//                records = avroConverter.convertCsvToAvro(fileStream, ";");
//                logger.info("Converted to {}", records);
//                googleCloudStorage.storeToBucket("test");
//            } catch (Exception e) {
//                logger.error("Something went wrong converting file to avro or writing records to storage: {} ", e.getMessage());
//                e.printStackTrace();
//            }
//
//            /** Test 3: Create inputstream from avrorecords and use googleCloudStorage to save it in storage **/
//            InputStream storeFileStream = channelSftp.get(WORKDIR + "/" + f.getFilename());
//            googleCloudStorage.writeInputStreamToStorage(storeFileStream, storageLocation+"file/storage_"+f.getFilename());

            saveFileRecord(f.getFilename());
        } catch (SftpException e) {
//        } catch (SftpException | IOException e) {
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
        logger.info("privateKeyFile: {}", tmpPrivateKeyFile);
        Files.write(Path.of(tmpPrivateKeyFile),
                    privatekeyfile.isEmpty() ?
                    privateKey.getBytes() : Files.readAllBytes(Path.of(privatekeyfile)));
//        logger.info("privatekey: {}", Files.readString(Path.of(tmpPrivateKeyFile)).substring(0,70));
//        logger.info("privatekey: {}", StringUtils.substring(Files.readString(Path.of(tmpPrivateKeyFile)),-50));
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



    /***********************/
    /** Just for testing ***/
    /***********************/
    public void list () {
        try {
            setupJsch();
            listFilesInPath(WORKDIR);
            listDirectory(WORKDIR);
        } catch (IOException e) {
            logger.error("IO-feil: {}", e.toString());
        } catch (SftpException e) {
            logger.error("Sftp-feil: {}", e.toString());
        } catch (JSchException e) {
            logger.error("jsch-feil: {}", e.toString());
        }
    }

    void listFilesInPath(String path) throws SftpException {
        fileList(path).forEach(f -> logger.info("file in {}: {}", path, f.getFilename()));
    }

    static void listDirectory(String path) throws SftpException {
        Vector<ChannelSftp.LsEntry> files = (Vector<ChannelSftp.LsEntry>)channelSftp.ls(path);
//        logger.info("List files in {}", path);
        for (ChannelSftp.LsEntry entry : files) {
//            logger.info("entry: {}", entry.toString());
            if (!entry.getAttrs().isDir()) {
//                logger.info("path: {}", path);
                logger.info("file: {}/{}" , path, entry.getFilename());
            } else {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    listDirectory(path + "/" + entry.getFilename());
                }
            }
        }
    }



}
