package no.ssb.forbruk.nets.filehandle.storage;

import no.ssb.forbruk.nets.filehandle.storage.utils.Encryption;
import no.ssb.forbruk.nets.metrics.MetricsManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

//@SpringJUnitConfig
@SpringBootTest
//@TestPropertySource(locations="classpath:application.yml")
public class GoogleCloudStorageTest {
    static final Logger logger = LoggerFactory.getLogger(GoogleCloudStorageTest.class);


    GoogleCloudStorage googleCloudStorageToTest = spy(new GoogleCloudStorage());

    @Value("${forbruk.nets.header}")
    static String headerLine;

    static MetricsManager metricsManager;


//    @BeforeAll
//    static void init() {
//        doReturn(mockEncryption).when(googleCloudStorageToTest).makeEncryption(anyString(), anyString(), anyString());
////        Mockito.when(googleCloudStorageToTest.makeEncryption(anyString(), anyString(), anyString())).thenReturn(
////                new Encryption("abc", "def", "true"));
//        googleCloudStorage.initialize(headerLine, metricsManager);
//    }

    @Test
    public void produceMessage_ok() throws IOException {
        Encryption encryption = new Encryption("abc", "def", "true");

//        doReturn(encryption).when(googleCloudStorageToTest.makeEncryption(anyString(), anyString(), anyString()));
        when(googleCloudStorageToTest.makeEncryption(anyString(), anyString(), anyString())).thenReturn(encryption);
        googleCloudStorageToTest.initialize(headerLine, metricsManager);


        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));

        logger.info("store {}", inputStream.readAllBytes().length);
        googleCloudStorageToTest.produceMessages(inputStream, "testNetsFile.csv");
        logger.info("read from bucket");
        googleCloudStorageToTest.consumeMessages();


    }
}
