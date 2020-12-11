package no.ssb.forbruk.nets.filehandle.storage;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.ssb.forbruk.nets.filehandle.storage.utils.Encryption;
import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.rawdata.api.RawdataConsumer;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.concurrent.TimeUnit;

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


    final static String headerLine = "TRANSAKSJONSDATO;TRANSAKSJONSTID;BRUKERSTED;KORTINNEH_KONTONR;VAREKJOP_BELOP;BELOEP_TOTALT;BR_STED_NAVN_TERM;BRUKERSTED_ORGNUMMER;BRUKERSTED_NAVN";

    final static Map configFileSystem = Map.of(
            "local-temp-folder", "local-temp-test",
            "avro-file.max.seconds","10",
            "avro-file.max.bytes", "10485760",
            "avro-file.sync.interval", "524288",
            "listing.min-interval-seconds", "0",
            "filesystem.storage-folder", "tmp/rawdata/nets"
    );

    final static Map configGcs = Map.of(
            "local-temp-folder", "local-temp-test",
            "avro-file.max.seconds","10",
            "avro-file.max.bytes", "10485760",
            "avro-file.sync.interval", "524288",
            "gcs.bucket-name", "nets-rawdata-staging-transactions",
            "gcs.listing.min-interval-seconds", "3",
            "gcs.credential-provider", "service-account",
            "gcs.service-account.key-file", "C://var//appdata//forbruk//ssb-team-forbruk-staging-d569602b5f9f.json"
    );

    String RAWDATA_TOPIC = "rawdataTopic";

    @Test
    public void test() throws IOException {

        googleCloudStorage.setRawdataClient(
                ProviderConfigurator.configure(configFileSystem, "filesystem", RawdataClientInitializer.class));
//                ProviderConfigurator.configure(configGcs, "gcs", RawdataClientInitializer.class));
        googleCloudStorage.setHeaderColumns((headerLine).split(";"));

        doNothing().when(encryption).setSecretKey();
        Counter counter = meterRegistry.counter("test");
        when(meterRegistry.counter(anyString(), anyCollection())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), anyInt())).thenReturn(1);

        ReflectionTestUtils.setField(googleCloudStorage, "rawdataTopic", RAWDATA_TOPIC);
        ReflectionTestUtils.setField(googleCloudStorage, "maxBufferLines", 3);
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));
        logger.info("inputstream: {}", inputStream.available());

        int antConsumedPre = consumeMessages(googleCloudStorage.getRawdataClient());

        int antTransactions = googleCloudStorage.produceMessages(inputStream, "test.csv");
        assertEquals(9, antTransactions);
        logger.info("read from bucket");
        int antConsumed = consumeMessages(googleCloudStorage.getRawdataClient());
        logger.info("finished handled file");
        assertEquals(antTransactions, antConsumed-antConsumedPre);

    }


    private int consumeMessages(RawdataClient rawdataClient) {
        int antConsumed = 0;
        try (RawdataConsumer consumer = rawdataClient.consumer(RAWDATA_TOPIC)) {
//            logger.info("consumer: {}", consumer.topic());
            RawdataMessage message;
            while ((message = consumer.receive(1, TimeUnit.SECONDS)) != null) {
//                logger.info("message position: {}", message.position());
                // print message
                StringBuilder contentBuilder = new StringBuilder();
                contentBuilder.append("\nposition: ").append(message.position());
                for (String key : message.keys()) {
//                    logger.info("key: {}", key);
//                    logger.info("  message content for key {}: {}", key, new String(message.get(key)));
                    contentBuilder
                            .append("\n\t").append(key).append(" => ")
                            .append(new String(message.get(key)));
                }
//                logger.info("consumed message {}", contentBuilder.toString());
                antConsumed++;
            }
        } catch (Exception e) {
            logger.error("Error consuming messages: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return antConsumed;
    }



}
