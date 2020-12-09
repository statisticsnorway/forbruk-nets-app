package no.ssb.forbruk.nets.filehandle.storage;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.ssb.forbruk.nets.filehandle.storage.utils.Encryption;
import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public class GoogleCloudStorageTest {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorageTest.class);

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Encryption encryption;

    @InjectMocks
    private GoogleCloudStorage googleCloudStorage;

//    final static String avrofileMaxSeconds = "10";
//    final static String avrofileMaxBytes = "10485760";
//    final static String avrofileSyncInterval =  "524288";
//    final static String storageBucket = "tmp/rawdata/nets";
//    final static String localTemFolder = "local-temp-test";
//    final static String storageProvider = "filesystem";

    final static String headerLine = "TRANSAKSJONSDATO;TRANSAKSJONSTID;BRUKERSTED;KORTINNEH_KONTONR;VAREKJOP_BELOP;BELOEP_TOTALT;BR_STED_NAVN_TERM;BRUKERSTED_ORGNUMMER;BRUKERSTED_NAVN";

//    final static Map configFileSystem = Map.of(
//            "local-temp-folder", "local-temp-test",
//            "avro-file.max.seconds","10",
//            "avro-file.max.bytes", "10485760",
//            "avro-file.sync.interval", "524288",
//            "listing.min-interval-seconds", "0",
//            "filesystem.storage-folder", "tmp/rawdata/nets"
//    );

    final static Map configGcs = Map.of(
            "local-temp-folder", "local-temp-test",
            "avro-file.max.seconds","10",
            "avro-file.max.bytes", "10485760",
            "avro-file.sync.interval", "524288",
            "gcs.bucket-name", "nets-rawdata-staging-transactions",
            "gcs.listing.min-interval-seconds", "3",
            "gcs.credential-provider", "compute-engine",
            "gcs.service-account.key-file", new String()
    );

    @Test
    public void test() throws IOException {
        googleCloudStorage.setRawdataClient(
                ProviderConfigurator.configure(configGcs, "gcs", RawdataClientInitializer.class));
        googleCloudStorage.setHeaderColumns((headerLine).split(";"));

        logger.info("configGcs: {}", configGcs);

        doNothing().when(encryption).setSecretKey();

        when(meterRegistry.counter(anyString(), anyCollection())).thenReturn(null);

        ReflectionTestUtils.setField(googleCloudStorage, "rawdataTopic", "test_transactions1");
        ReflectionTestUtils.setField(googleCloudStorage, "maxBufferLines", 3);
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));
        logger.info("inputstream: {}", inputStream.available());

        int antTransactions = googleCloudStorage.produceMessages(inputStream, "test.csv");
        assertEquals(9, antTransactions);
    }

}
