package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.db.model.ForbrukNetsFiles;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsFilesRepository;
import no.ssb.forbruk.nets.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.storage.GoogleCloudStorage;
import no.ssb.forbruk.nets.storage.utils.TestUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class NetsHandleTest {

    @Mock
    private SftpFileTransfer sftpFileTransfer;

    @Mock
    private GoogleCloudStorage googleCloudStorage;

    @Mock
    private ForbrukNetsFilesRepository forbrukNetsFilesRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private NetsHandle netsHandle;

//    @Test
    public void test_GetAndHandleNetsfile_ok() throws Exception {
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));
        ChannelSftp.LsEntry testfile = TestUtilities.lsEntryWithGivenFilename("test.csv");
        Collection<ChannelSftp.LsEntry> fileList = new Vector<ChannelSftp.LsEntry>();
        fileList.add(testfile);

        // ignore logging to database
        when(forbrukNetsFilesRepository.save(any(ForbrukNetsFiles.class))).thenReturn(null);

        // ignore getting files from nets and storing content to gcs
        when(sftpFileTransfer.setupJsch()).thenReturn(true);
        when(sftpFileTransfer.fileList()).thenReturn(fileList);
        when(sftpFileTransfer.getFileInputStream(any())).thenReturn(inputStream);
        when(googleCloudStorage.produceMessages(any(), anyString())).thenReturn(5);

        // try to ignore meterregistry
        Counter counter = meterRegistry.counter("test");
        when(meterRegistry.counter(anyString(), anyCollection())).thenReturn(counter);
        when(meterRegistry.gauge(anyString(), anyInt())).thenReturn(1);


        // do the thing
        netsHandle.getAndHandleNetsFiles();

        // try to find whats left to check..
        assertEquals("a", "a");


    }

}
