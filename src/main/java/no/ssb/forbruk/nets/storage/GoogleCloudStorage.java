package no.ssb.forbruk.nets.storage;


import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.ssb.forbruk.nets.storage.utils.Encryption;
import no.ssb.forbruk.nets.storage.utils.Manifest;
import no.ssb.forbruk.nets.storage.utils.ULIDGenerator;
import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.rawdata.api.RawdataProducer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class GoogleCloudStorage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    @Value("${storage.provider}")
    private String storageProvider;
    @Value("${google.storage.provider.bucket}")
    private String storageBucket;
    @Value("${google.storage.secret.keyfile}")
    private String storageSecretFile;
    @Value("${google.storage.credential.provider}")
    private String credentialProvider;
    @Value("${google.storage.local.temp.folder}")
    private String localTemFolder;
    @Value("${google.storage.provider.topic}")
    private String rawdataTopic;
    @Value("${google.storage.buffer.lines}")
    private int maxBufferLines;

    @Value("${forbruk.nets.header}")
    private String headerLine;

    @NonNull
    private final Encryption encryption; //= new Encryption(encryptionKey, encryptionSalt, encrypt); // TODO: Sjekk om dette funker.

    @NonNull
    private final MeterRegistry meterRegistry;

    @Setter @Getter
    static RawdataClient rawdataClient;
    @Setter @Getter
    static String [] headerColumns;

    private final static String avrofileMaxSeconds = "10";
    private final static String avrofileMaxBytes = "10485760";
    private final static String avrofileSyncInterval =  "524288";


    @Counted(value="forbruk_nets_app_cloudstorageinitialize", description="count googlecloudstorage initializing")
    public void setupGoogleCloudStorage() {
        Map<String, String> configuration = Map.of(
                "local-temp-folder", localTemFolder,
                "avro-file.max.seconds", avrofileMaxSeconds,
                "avro-file.max.bytes", avrofileMaxBytes,
                "avro-file.sync.interval", avrofileSyncInterval,
                "gcs.bucket-name", storageBucket,
                "gcs.listing.min-interval-seconds", "3",
                "gcs.credential-provider", credentialProvider,
                "gcs.service-account.key-file", storageSecretFile);

        logger.info("cofiguration: {}", configuration);
        encryption.setEncryptionValues();

        rawdataClient = ProviderConfigurator.configure(configuration,
                storageProvider, RawdataClientInitializer.class);

        headerColumns = headerLine.split(";");
    }

    @Timed(value="forbruk_nets_app_producemessages", description="Time store transactions for one file")
    public int produceMessages(InputStream inputStream, String filename) throws Exception {

        int totalTransactions = 0;
        try (RawdataProducer producer = rawdataClient.producer(rawdataTopic)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final AtomicBoolean skipHeader = new AtomicBoolean(false);
            final List<String> positions = new ArrayList<>();
            String line;
            // loop through all lines in inputstream, create storagemessage for each, buffer the message
            // and publish for each <maxbuffer> message
            while ((line = reader.readLine()) != null) {
                logger.info("line: {}", line);
                if (skipHeader.getAndSet(true)) {
                    //create unique position for fileline
                    String position = ULIDGenerator.toUUID(ULIDGenerator.generate()).toString();

                    //create message and buffer it
                    producer.buffer(
                            createMessageWithManifestAndEntry(filename, producer, line, position));
                    positions.add(position);

                    // publish every maxBufferLines lines
                    if (positions.size() >= maxBufferLines) {
                        totalTransactions += publishPositions(producer, positions);
                        positions.clear();
                        logger.info("positions: {}", positions);
                    }
                }

            }
            // publish the last positions for file
            totalTransactions += positions.size() > 0 ?
                    publishPositions(producer, positions) : 0;

        } catch (Exception e) {
            meterRegistry.counter("forbruk_nets_app_error_rawdataproducer", "error", "publish messages").increment();
            throw new Exception(e);
        }
        return totalTransactions;
    }

    private RawdataMessage.Builder createMessageWithManifestAndEntry(String filename, RawdataProducer producer, String line, String position) {
        byte[] manifestJson = Manifest.generateManifest(
                producer.topic(), position, line.length(),
                headerColumns, filename);

        RawdataMessage.Builder messageBuilder = producer.builder();
        messageBuilder.position(position);

        messageBuilder.put("manifest.json", encryption.tryEncryptContent(manifestJson));
        messageBuilder.put("entry", encryption.tryEncryptContent(line.getBytes()));
        return messageBuilder;
    }

    @Timed(value="forbruk_nets_app_publishpositions", description = "publish positions")
    protected int publishPositions(RawdataProducer producer, List<String> positions) {
        logger.info("publish {} positions", positions.size());
        producer.publish(positions.toArray(new String[0]));
        meterRegistry.gauge("forbruk_nets_app_transactions", positions.size());
        meterRegistry.counter("forbruk_nets_app_total_transactions", "count", "transactions stored").increment(positions.size());

        return positions.size();
    }

}
