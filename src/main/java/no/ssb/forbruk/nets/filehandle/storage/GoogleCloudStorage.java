package no.ssb.forbruk.nets.filehandle.storage;


import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.storage.utils.Encryption;
import no.ssb.forbruk.nets.filehandle.storage.utils.Manifest;
import no.ssb.forbruk.nets.filehandle.storage.utils.ULIDGenerator;
import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.rawdata.api.RawdataConsumer;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.api.RawdataProducer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GoogleCloudStorage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    @Value("${storage.provider}")
    String storageProvider;
    @Value("${google.storage.provider.bucket}")
    String storageBucket;
    @Value("${google.storage.secret.keyfile}")
    String storageSecretFile;
    @Value("${google.storage.credential.provider}")
    String credentialProvider;
    @Value("${google.storage.local.temp.folder}")
    String localTemFolder;
    @Value("${google.storage.provider.topic}")
    String rawdataTopic;
    @Value("${google.storage.buffer.lines}")
    int maxBufferLines;

    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;
    @Value("${google.storage.encryption}")
    private String encrypt;

    Encryption encryption;

    @Autowired
    MeterRegistry meterRegistry;


    Map<String, String> configuration;
    static RawdataClient rawdataClient;
    static String [] headerColumns;

    final static String avrofileMaxSeconds = "10";
    final static String avrofileMaxBytes = "10485760";
    final static String avrofileSyncInterval =  "524288";


    @Counted(value="forbruk_nets_app_cloudstorageinitialize", description="count googlecloudstorage initializing")
    public void initialize(String headerLine) {
        encryption = new Encryption(encryptionKey, encryptionSalt, encrypt);
//        logger.info(encryption.toString());
//        logger.info("storageProvider: {}", storageProvider);
//        logger.info("storageBucket: {}", storageBucket);
//        logger.info("localTemFolder: {}", localTemFolder);
//        logger.info("credentialProvider: {}", credentialProvider);
//        logger.info("storageSecretFile: {}", storageSecretFile);
//        logger.info("rawdataTopic: {}", rawdataTopic);

        setConfiguration(storageProvider);
        rawdataClient = ProviderConfigurator.configure(configuration,
                storageProvider, RawdataClientInitializer.class);
//        logger.info("rawdataClient: {}", rawdataClient.toString());

        headerColumns = headerLine.split(";");
//        this.metricsManager = metricsManager;
    }

    @Timed(value="forbruk_nets_app_producemessages", description="Time store transactions for one file")
    public int produceMessages(InputStream inputStream, String filename) {
        int totalTransactions = 0;
        try (RawdataProducer producer = rawdataClient.producer(rawdataTopic)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            AtomicBoolean skipHeader = new AtomicBoolean(false);
            List<String> positions = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (skipHeader.getAndSet(true)) {
                        String position = ULIDGenerator.toUUID(ULIDGenerator.generate()).toString();

                        byte[] manifestJson = Manifest.generateManifest(
                                producer.topic(), position, line.length(),
                                headerColumns, filename);

                        RawdataMessage.Builder messageBuilder = producer.builder();
                        messageBuilder.position(position);

                        messageBuilder.put("manifest.json", encryption.tryEncryptContent(manifestJson));
                        messageBuilder.put("entry", encryption.tryEncryptContent(line.getBytes()));
                        producer.buffer(messageBuilder);

                        positions.add(position);

                        // publish every maxBufferLines lines
                        if (positions.size() >= maxBufferLines) {
                            totalTransactions += publishPositions(producer, positions);
                            positions = new ArrayList<>();
                        }
                    }
                } catch (Exception e) {
                    meterRegistry.counter("forbruk_nets_app_error_producemessages", "error", "produce messages");
                    logger.error("Error creating or buffering message for line {}", line);
                }

            }
            // publish the last positions for file
            totalTransactions += positions.size() > 0 ?
                    publishPositions(producer, positions) : 0;

        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_rawdataproducer", "error", "generate rawdataproducer");
            logger.error("Error creating rawdataproducer for {}: {}", filename, e.getMessage());
            e.printStackTrace();
        }
        return totalTransactions;
    }

    @Timed(value="forbruk_nets_app_publishpositions", description = "publish positions")
    protected int publishPositions(RawdataProducer producer, List<String> positions) {
        try {
            logger.info("publish {} positions", positions.size());
            producer.publish(positions.toArray(new String[0]));
            meterRegistry.gauge("forbruk_nets_app_transactions", positions.size());
            meterRegistry.counter("forbruk_nets_app_total_transactions", "count", "transactions stored").increment(positions.size());
            return positions.size();
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_publischpositions", "error", "store transaction");
            logger.error("something went wrong when publishing positions \n\t {} to {}", positions.get(0), positions.get(positions.size()-1));
            return 0;
        }


    }


    //TODO: Move this to test
    @Timed(value="forbruk_nets_app_consumemessages", description="consume messages")
    public void consumeMessages() {
        try (RawdataConsumer consumer = rawdataClient.consumer(rawdataTopic)) {
            logger.info("consumer: {}", consumer.topic());
            RawdataMessage message;
            while ((message = consumer.receive(1, TimeUnit.SECONDS)) != null) {
//                logger.info("message position: {}", message.position());
                // print message
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append("\nposition: ").append(message.position());
                for (String key : message.keys()) {
//                    logger.info("key: {}", key);
//                    logger.info("  message content for key {}: {}", key, new String(message.get(key)));
//                    logger.info("dekryptert mess: {}", new String(encryption.tryDecryptContent(message.get(key))));
                    contentBuilder
                            .append("\n\t").append(key).append(" => ")
                            .append(new String(encryption.tryDecryptContent(message.get(key))));
                }
//                logger.info("consumed message {}", contentBuilder.toString());
            }
        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_consumemessages", "error", "consume messages");
            logger.error("Error consuming messages: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private void setConfiguration(String storageProvider) {
        configuration = "gcs".equals(storageProvider) ?
                Map.of(
                        "local-temp-folder", localTemFolder,
                        "avro-file.max.seconds", avrofileMaxSeconds,
                        "avro-file.max.bytes", avrofileMaxBytes,
                        "avro-file.sync.interval", avrofileSyncInterval,
                        "gcs.bucket-name", storageBucket,
                        "gcs.listing.min-interval-seconds", "3",
                        "gcs.credential-provider", credentialProvider,
                        "gcs.service-account.key-file", storageSecretFile)
                :
                Map.of(
                        "local-temp-folder", localTemFolder,
                        "avro-file.max.seconds", avrofileMaxSeconds,
                        "avro-file.max.bytes", avrofileMaxBytes,
                        "avro-file.sync.interval", avrofileSyncInterval,
                        "listing.min-interval-seconds", "0",
                        "filesystem.storage-folder", storageBucket
                        )
                ;
//                configuration.forEach((k,v) -> logger.info("config {}:{}", k, v));
    }

}
