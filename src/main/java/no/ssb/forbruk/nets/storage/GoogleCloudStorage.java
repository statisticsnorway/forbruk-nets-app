package no.ssb.forbruk.nets.storage;

import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend.Configuration;
import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import no.ssb.rawdata.api.storage.RawdataClient;
import no.ssb.rawdata.api.storage.RawdataClientInitializer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class GoogleCloudStorage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    private DynamicConfiguration dynamicConfiguration;
    private RawdataClient rawdataClient;


    public GoogleCloudStorage() {
        dynamicConfiguration = configuration();
        logger.info("configs: {}", dynamicConfiguration.asMap().size());
        dynamicConfiguration.asMap().forEach((k, v) -> logger.info("config: {}={}", k, v));
        rawdataClient = storageProvider(dynamicConfiguration);
        logger.info("return googleCloudStorage");
    }

    static DynamicConfiguration configuration() {
//        Path currentPath = Paths.get("").toAbsolutePath().resolve("target");
        return new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource("application.properties")
                .values("storage.provider", "gcp")
                .values("google.cloud.provider.bucket", "nets-rawdata-staging-transactions")
                .build();
    }

    static RawdataClient storageProvider(DynamicConfiguration configuration) {
        return ProviderConfigurator.configure(configuration, "gcp", RawdataClientInitializer.class);
    }

    public void writeData() {
        byte[] rawdata = "foo".getBytes();
        rawdataClient.write("ns", "1", "file-1.txt", rawdata);
    }


    /** Er det mulig å bruke dapla-storage-client? Eller er det kun mot dapla-rawdata? **/
    public void writeInputStreamToBucket(InputStream inputStream) throws IOException {
        Configuration configuration = new Configuration();
//        configuration.setServiceAccountCredentials(Path.of("gcs-secret/gcs_sa_dev.json"));
        GoogleCloudStorageBackend backend = new GoogleCloudStorageBackend(configuration);

        try (InputStream is = Files.newInputStream(Path.of("src/test/resources/1031-bytes.zip"))) {
            backend.write("gs://ssb-rawdata-dev/tmp/test.zip", is);
        }
    }
}
