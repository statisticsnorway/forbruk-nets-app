package no.ssb.forbruk.nets.storage;


import no.ssb.forbruk.nets.storage.utils.Encryption;
import no.ssb.forbruk.nets.storage.utils.Manifest;
import no.ssb.forbruk.nets.storage.utils.ULIDGenerator;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    Encryption encryption;

    Map<String, String> configuration;
    static RawdataClient rawdataClient;
    static String [] headerColumns;

    public void initialize(String headerLine) {
//        logger.info("storageProvider: {}", storageProvider);
//        logger.info("storageBucket: {}", storageBucket);
//        logger.info("localTemFolder: {}", localTemFolder);
//        logger.info("credentialProvider: {}", credentialProvider);
//        logger.info("storageSecretFile: {}", storageSecretFile);
//        logger.info("rawdataTopic: {}", rawdataTopic);

        String storageLocation = "filesystem".equals(storageProvider) ? storageBucket : "gs://" + storageBucket + "/";
        logger.info("storageLocation: {}", storageLocation);

        this.configuration = Map.of(
                "local-temp-folder", localTemFolder,
                "avro-file.max.seconds", "10",
                "avro-file.max.bytes", "10485760",
                "avro-file.sync.interval", "524288",
                "gcs.bucket-name", storageLocation,
                "gcs.listing.min-interval-seconds", "3",
                "gcs.credential-provider", credentialProvider,
                "gcs.service-account.key-file", storageSecretFile
//                "listing.min-interval-seconds", "0",
//                "filesystem.storage-folder", storageLocation
                );
        logger.info("konfig: {}", this.configuration);
        rawdataClient = ProviderConfigurator.configure(configuration,
                storageProvider, RawdataClientInitializer.class);
        logger.info("rawdataClient: {}", rawdataClient.toString());

//        encryption = new Encryption();
        encryption.initialize();
        headerColumns = headerLine.split(";");

    }

    public void produceMessages(String filePath) throws Exception {
        try (RawdataProducer producer = rawdataClient.producer(rawdataTopic)) {

            try {
                List<String> positions = new ArrayList<>();
                List<String> lines = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
                lines.remove(0);
                lines.forEach(line -> {
//                logger.info("fillinje: {}", line);
//                logger.info("kryptert: {}", new String(encryption.tryEncryptContent(line.getBytes())));

                    String position = ULIDGenerator.toUUID(ULIDGenerator.generate()).toString();

                    byte[] manifestJson = Manifest.generateManifest(
                            producer.topic(), position, line.length(), headerColumns, filePath);
                    logger.info("manifest: {}", new String(manifestJson));


                    RawdataMessage.Builder messageBuilder = producer.builder();
                    messageBuilder.position(position);
//                    messageBuilder.put("manifest.json", encryption.tryEncryptContent(manifestJson));
//                    messageBuilder.put("entry", encryption.tryEncryptContent(line.getBytes()));
                    messageBuilder.put("manifest.json", manifestJson);
                    messageBuilder.put("entry", line.getBytes());
                    producer.buffer(messageBuilder);

                    positions.add(position);
                });
                logger.info("positions: {}", positions);
                String[] publishPositions = positions.toArray(new String[positions.size()]);

//                logger.info("Publish positions: {}", publishPositions);
//                for (String k: publishPositions) {
//                    logger.info("pos: {}", k);
//                }
                producer.publish(publishPositions);

            } catch (IOException io) {
                logger.error("Error reading file {}: {}", filePath, io.getMessage());
                io.printStackTrace();
            }
        } catch (Exception e) {
            logger.error("Error creating rawdataproducer for {}: {}", filePath, e.getMessage());
            e.printStackTrace();
        }
    }


    public void readFromBucket() {
        try {
            Thread consumerThread = new Thread(() -> consumeMessages());
            consumerThread.start();

            consumerThread.join();
        } catch (InterruptedException e) {
            logger.error("InterruptedException in readFromBucket: {}", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Exception in readFromBucket: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    void consumeMessages() {
        try (RawdataConsumer consumer = rawdataClient.consumer(rawdataTopic)) {
            logger.info("consumer: {}", consumer.topic());
            RawdataMessage message;
            while ((message = consumer.receive(1, TimeUnit.SECONDS)) != null) {
                logger.info("message position: {}", message.position());
                // print message
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append("\nposition: ").append(message.position());
                for (String key : message.keys()) {
                    contentBuilder
                            .append("\n\t").append(key).append(" => ")
                            .append(new String(message.get(key)));
//                            .append(new String(encryption.tryDecryptContent(message.get(key))));
                }
                logger.info("consumed message {}", contentBuilder.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
