package no.ssb.forbruk.nets.storage;


import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.rawdata.api.RawdataConsumer;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.api.RawdataProducer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.compress.utils.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GoogleCloudStorage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    @Value("#{environment.forbruk_nets_encryption_key}")
    private String encryptionKey;
    @Value("#{environment.forbruk_nets_encryption_salt}")
    private String encryptionSalt;

    @Value("${google.storage.provider.bucket}")
    private String storageBucket;
    @Value("${google.storage.secret.keyfile}")
    private String storageSecretFile;
    @Value("${google.storage.credential.provider}")
    private String credentialProvider;
    @Value("${google.storage.local.temp.folder}")
    private String localTemFolder;

    Map<String, String> configuration;
    static RawdataClient rawdataClient;
    static String rawdataStream;

    public void initialize(String runEnvironment, String rawdataStream) {
        String storageLocation = "local".equals(runEnvironment) ? storageBucket : "gs://" + storageBucket + "/";
        this.configuration = Map.of(
                "local-temp-folder", localTemFolder,
                "avro-file.max.seconds", "30",
                "avro-file.max.bytes", "10485760",
                "avro-file.sync.interval", "524288",
                "gcs.bucket-name", storageLocation,
                "gcs.listing.min-interval-seconds", "3",
                "gcs.credential-provider", credentialProvider,
                "gcs.service-account.key-file", storageSecretFile
//                "listing.min-interval-seconds", "0",
//                "filesystem.storage-folder", "tmp/rawdata/storage"
                );
        rawdataClient = ProviderConfigurator.configure(configuration,
                "gcs", RawdataClientInitializer.class);
        this.rawdataStream = rawdataStream;
        logger.info("storagebucket: {}", storageBucket);

        logger.info("Config: {}", this.configuration);

    }

    public void storeToBucket(String storage) {
        try {
            Thread consumerThread = new Thread(() -> consumeMessages());
            consumerThread.start();

            produceMessages(storage);

            consumerThread.join();
        } catch (InterruptedException e) {
            logger.error("InterruptedException in storeToBucket: {}", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Exception in storeToBucket: {}", e.getMessage());
            e.printStackTrace();
        }
    }


    static void consumeMessages() {
        try (RawdataConsumer consumer = rawdataClient.consumer(rawdataStream)) {
            logger.info("consumer: {}", consumer.topic());
            for (; ; ) {
                RawdataMessage message = consumer.receive(30, TimeUnit.SECONDS);
                logger.info("message position: {}", message.position());
                if (message != null) {
                    logger.info("Consumed message with id: {}", message.ulid());
                    if (message.position().equals("10")) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void produceMessages(String filePath) throws Exception {
        try (RawdataProducer producer = rawdataClient.producer(rawdataStream)) {
            AtomicInteger i = new AtomicInteger(0);
            Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8).forEach(line -> {
                logger.info("fillinje: {}", line);
                producer.publishBuilders(producer.builder().position(String.valueOf(i.getAndAdd(1)))
                        .put("the-payload", line.getBytes()));

            });
            producer.publishBuilders(producer.builder().position(String.valueOf(i.getAndAdd(1)))
                    .put("metadata", ("created-time " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error("Error creating rawdataproducer for {}: {}", filePath, e.getMessage());
            e.printStackTrace();
        }
    }
}
