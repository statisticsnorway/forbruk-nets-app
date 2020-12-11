package no.ssb.forbruk.nets.filehandle.storage;


import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.ssb.forbruk.nets.db.model.service.ForbrukNetsLogService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
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

    @Value("${forbruk.nets.header}")
    String headerLine;

    @NonNull
    Encryption encryption; //= new Encryption(encryptionKey, encryptionSalt, encrypt); // TODO: Sjekk om dette funker.

    @NonNull
    final MeterRegistry meterRegistry;

    @NonNull
    final ForbrukNetsLogService forbrukNetsLogService;


    @Setter @Getter
    static RawdataClient rawdataClient;
    @Setter @Getter
    static String [] headerColumns;

    final static String avrofileMaxSeconds = "10";
    final static String avrofileMaxBytes = "10485760";
    final static String avrofileSyncInterval =  "524288";


    @Counted(value="forbruk_nets_app_cloudstorageinitialize", description="count googlecloudstorage initializing")
    public void initialize() {
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
        encryption.initialize();

        rawdataClient = ProviderConfigurator.configure(configuration,
                storageProvider, RawdataClientInitializer.class);

        headerColumns = headerLine.split(";");
    }

    @Timed(value="forbruk_nets_app_producemessages", description="Time store transactions for one file")
    public int produceMessages(InputStream inputStream, String filename) {

        int totalTransactions = 0;
        try (RawdataProducer producer = rawdataClient.producer(rawdataTopic)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final AtomicBoolean skipHeader = new AtomicBoolean(false);
            final List<String> positions = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (skipHeader.getAndSet(true)) {
                        //create unique position for fileline
                        String position = ULIDGenerator.toUUID(ULIDGenerator.generate()).toString();

                        //create message and buffer it
                        producer.buffer(
                                createMessageWithManifestAndEntry(filename, producer, line, position));
                        positions.add(position);

                        // publish every maxBufferLines lines
                        if (positions.size() >= maxBufferLines) {
                            totalTransactions += publishPositions(producer, positions, filename);
                            positions.clear();
                            logger.info("positions: {}", positions);
                        }
                    }
                } catch (Exception e) {
//                    meterRegistry.counter("forbruk_nets_app_error_producemessages", "error", "produce messages").increment();
                    logger.error("Error creating or buffering message for line {}: {}", line, e.getMessage());
                    forbrukNetsLogService.saveLogError(filename, "forbruk_nets_app_error_producemessages", 1L);
                }

            }
            // publish the last positions for file
            totalTransactions += positions.size() > 0 ?
                    publishPositions(producer, positions, filename) : 0;

        } catch (Exception e) {
//            meterRegistry.counter("forbruk_nets_app_error_rawdataproducer", "error", "generate rawdataproducer").increment();
            logger.error("Error creating rawdataproducer for {}: {}", filename, e.getMessage());
            e.printStackTrace();
            forbrukNetsLogService.saveLogError(filename, "forbruk_nets_app_error_rawdataproducer", 1L);
        }
        forbrukNetsLogService.saveLogOK(filename, "total transactions saved", Long.valueOf(totalTransactions));
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
    protected int publishPositions(RawdataProducer producer, List<String> positions, String filename) {
        try {
            logger.info("publish {} positions", positions.size());
            producer.publish(positions.toArray(new String[0]));
            forbrukNetsLogService.saveLogOK(filename, "published positions", Long.valueOf(positions.size()));
            meterRegistry.gauge("forbruk_nets_app_transactions", positions.size());
//            meterRegistry.counter("forbruk_nets_app_total_transactions", "count", "transactions stored").increment(positions.size());

            return positions.size();
        } catch (Exception e) {
//            meterRegistry.counter("forbruk_nets_app_error_publishpositions", "error", "store transaction").increment();
            logger.error("something went wrong when publishing positions \n\t {} to {}, {}", positions.get(0), positions.get(positions.size()-1), e);
            forbrukNetsLogService.saveLogError(filename, "not published positions", Long.valueOf(positions.size()));
            return 0;
        }


    }
}
