package no.ssb.forbruk.nets.storage;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import no.ssb.dapla.dataset.uri.DatasetUri;
import no.ssb.dapla.storage.client.DatasetStorage;
import no.ssb.dapla.storage.client.backend.BinaryBackend;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend;
import no.ssb.dapla.storage.client.backend.gcs.GoogleCloudStorageBackend.Configuration;
import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import no.ssb.dapla.storage.client.backend.local.LocalBackend;
import no.ssb.rawdata.api.storage.RawdataClient;
import no.ssb.rawdata.api.storage.RawdataClientInitializer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;

public class GoogleCloudStorage {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorage.class);

    private BinaryBackend backend;

    private DynamicConfiguration dynamicConfiguration;
    private RawdataClient rawdataClient;

    /********************* GoogleCloudStorageBackend *********************************/
    public GoogleCloudStorage(String env) {
        logger.info("env: {}", env);
        if ("cloud".equals(env)) {
            Configuration configuration = new Configuration();
//        configuration.setServiceAccountCredentials(Path.of("gcs-secret/gcs_sa_dev.json"));
            backend = new GoogleCloudStorageBackend(configuration);
        } else {
            backend = new LocalBackend();
        }
    }

    public void writeInputStreamToStorage(InputStream inputStream,  String storageLocation) throws IOException {
        logger.info("backend-class: {}", backend.getClass().getSimpleName());
        if (isLocalBackend()) {
            writeInputStreamToLocal(inputStream, storageLocation);
        } else {
            writeInputStreamToBucket(inputStream, storageLocation);
        }
    }

    /** Er det mulig å bruke dapla-storage-client? Eller er det kun mot dapla-rawdata? **/
    private void writeInputStreamToBucket(InputStream inputStream, String bucket) throws IOException {
        ((GoogleCloudStorageBackend)backend).write(bucket, inputStream);
        logger.info("stored inputstream to bucket {}", bucket);
        SeekableByteChannel read = backend.read(bucket);
        logger.info("read from bucket: {}", read.toString());
    }

    private void writeInputStreamToLocal(InputStream inputStream, String storageLocation) throws IOException {
        logger.info("write inputstream to file {}", (new File(storageLocation)).toURI().toString());
        backend.write((new File(storageLocation)).toURI().toString(), inputStream.readAllBytes());
        logger.info("written to file  {}: ", storageLocation);
//        Files.readAllLines(Paths.get(storageLocation)).forEach(l -> logger.info("  {}", l));
    }

    public void writeRecordsToStorage(List<GenericRecord> records, Schema schema, String storageLocation) {
        DatasetStorage storageClient = DatasetStorage.builder()
                .withWriteExceptionHandler((e, record) -> Optional.empty())
                .withBinaryBackend(backend)
                .build();
        logger.info("backend-class: {}", backend.getClass().getSimpleName());
        logger.info("storageLocation: {}", storageLocation);

        try {
            Schema testSchema = new Schema.Parser().parse("""
                    {
                      "type": "record",
                      "name": "person",
                      "fields": [
                        {
                          "name": "address",
                          "type": [
                            "null",
                            {
                              "type": "array",
                              "items": "string"
                            }
                          ],
                          "default": null
                        }
                      ]
                    }""");
            logger.info("create testUri");
            DatasetUri testUri = DatasetUri.of(
                    isLocalBackend() ?
                            (new File(storageLocation)).toURI().toString() : storageLocation
                    , "test", "1");
            logger.info("create testrecords");
            Flowable<GenericData.Record> testRecords = asFlowable(
                    new GenericRecordBuilder(testSchema).set("address", singletonList("1")).build(),
                    new GenericRecordBuilder(testSchema).set("address", Arrays.asList("2", null, "foo")).build(),
                    new GenericRecordBuilder(testSchema).set("address", singletonList("3")).build()
            );
            logger.info("write testRecords");
            storageClient.writeDataUnbounded(testUri, testSchema, testRecords, 300, TimeUnit.SECONDS, 1000)
                    .subscribe(record -> {
                    }, throwable -> {
                    });
            logger.info("read test records");
            try {
                List<GenericRecord> got = storageClient.readAvroRecords(testUri);
                got.forEach(r -> logger.info("  r: {}", r.toString()));
            } catch (Exception e) {
                logger.error("feil lesing av records: {}", e.getMessage());
            }
            logger.info("ferdig test records");
        } catch (Exception e) {
            logger.error("feil i test: {}", e.getMessage());
        }




        try {
            DatasetUri uri = DatasetUri.of(
                    isLocalBackend() ?
                            (new File(storageLocation)).toURI().toString() : storageLocation
                    , "nets/test", "1");

            Flowable<GenericRecord> flowableRecords = Flowable.fromIterable(records);
//            Write records
            logger.info("write flowable records to {}", uri.toString());
            storageClient.writeDataUnbounded(uri, schema, flowableRecords, 300, TimeUnit.SECONDS, 10)
                    .subscribe(record -> {
                    }, throwable -> {
                    });


//            Completable completable = storageClient.writeAllData(uri, schema, flowableRecords);
//            completable.doOnComplete(() -> {
//                logger.info("records written");
                readFromDatasetUri(storageClient, uri);
//            }).doOnError((throwable) ->
//                logger.error("something went wrong in writing records: {}", throwable.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating datasetUri of saving records: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void readFromDatasetUri(DatasetStorage storageClient, DatasetUri uri) {
        try {
            List<GenericRecord> got = storageClient.readAvroRecords(uri);
            logger.info("records read from storage {} ({})", uri.toString(), got.size());
            got.forEach(l -> logger.info("   saved: {}", l.get(3)));
        } catch (Exception e) {
            logger.error("Error reading saved avrorecords from storage ({}): ", uri.toString(), e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isLocalBackend() {
        return "LocalBackend".equals(backend.getClass().getSimpleName());
    }




    /********************* RawdataClient *********************************/
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
                .propertiesResource("application.yml")
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


    // TODO: remove this
    private static Flowable<GenericData.Record> asFlowable(GenericData.Record... records) {
        AtomicInteger counter = new AtomicInteger(0);
        return Flowable.generate(emitter -> {
            if (counter.get() < records.length) {
                emitter.onNext(records[counter.getAndIncrement()]);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
            } else {
                emitter.onComplete();
            }
        });
    }

}
