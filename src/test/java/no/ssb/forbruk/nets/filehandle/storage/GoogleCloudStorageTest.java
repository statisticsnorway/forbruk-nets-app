package no.ssb.forbruk.nets.filehandle.storage;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.ssb.forbruk.nets.db.model.service.ForbrukNetsLogService;
import no.ssb.forbruk.nets.filehandle.storage.utils.Encryption;
import no.ssb.rawdata.api.RawdataClient;
import no.ssb.rawdata.api.RawdataClientInitializer;
import no.ssb.rawdata.api.RawdataConsumer;
import no.ssb.rawdata.api.RawdataMessage;
import no.ssb.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Mock
    private ForbrukNetsLogService forbrukNetsLogService;

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
        //set googleCloudStorage's rawdataClient and headerColumns
        googleCloudStorage.setRawdataClient(
                // use gcs when testing in local intelliJ on windows because filesystem-filenames given in
                // rawdata-client-provider-gcs includes semicolon which "windows" doesn't approve (AvroRawdataUtils)
                 ProviderConfigurator.configure(configFileSystem, "filesystem", RawdataClientInitializer.class));
//                ProviderConfigurator.configure(configGcs, "gcs", RawdataClientInitializer.class));
        googleCloudStorage.setHeaderColumns((headerLine).split(";"));

        //mock saving to database
        doNothing().when(forbrukNetsLogService).saveLogOK(anyString(), anyString(), anyLong());
        doNothing().when(forbrukNetsLogService).saveLogError(anyString(), anyString(), anyLong());

        //mock metrics
//        doNothing().when(meterRegistry.counter(anyString(), any(String[].class))).increment();
        Counter counter = meterRegistry.counter("test","count", "something");
        when(meterRegistry.counter(anyString(), anyIterable())).thenReturn(counter);

//        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), anyInt())).thenReturn(1);

        //mock encryption
        when(encryption.tryEncryptContent(any(byte[].class))).then(returnsFirstArg());

        //set properties which should be read from propertiesfile
        ReflectionTestUtils.setField(googleCloudStorage, "rawdataTopic", RAWDATA_TOPIC);
        ReflectionTestUtils.setField(googleCloudStorage, "maxBufferLines", 3);

        //set initial messages in storage
        int antConsumedPre = consumeMessages(googleCloudStorage.getRawdataClient());

        //create test-file-inputstream to store
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));

        //do the thing
        int antTransactions = googleCloudStorage.produceMessages(inputStream, "test.csv");

        //check
        //TODO: get the mocking of meterregistry ok so the produceMessages can work
        assertEquals(9, antTransactions); //expected should be 9
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
