package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.db.model.service.ForbrukNetsLogService;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsLogRepository;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import no.ssb.forbruk.nets.filehandle.storage.utils.TestUtilities;
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
    private ForbrukNetsLogService forbrukNetsLogService;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private NetsHandle netsHandle;

    @Test
    public void testTest() throws IOException, SftpException, JSchException {
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));
        ChannelSftp.LsEntry testfile = TestUtilities.lsEntryWithGivenFilename("test.csv");
        Collection<ChannelSftp.LsEntry> fileList = new Vector<ChannelSftp.LsEntry>();
        fileList.add(testfile);

        doNothing().when(forbrukNetsLogService).saveLogOK(anyString(), anyString(), anyLong());
        doNothing().when(forbrukNetsLogService).saveLogError(anyString(), anyString(), anyLong());

        when(sftpFileTransfer.setupJsch()).thenReturn(true);
        when(sftpFileTransfer.fileList()).thenReturn(fileList);
        when(sftpFileTransfer.getFileInputStream(any())).thenReturn(inputStream);
        when(googleCloudStorage.produceMessages(any(), anyString())).thenReturn(5);

        Counter counter = meterRegistry.counter("test");
        when(meterRegistry.counter(anyString(), anyCollection())).thenReturn(counter);//.increment(anyInt());
        when(meterRegistry.gauge(anyString(), anyInt())).thenReturn(1);


        netsHandle.getAndHandleNetsFiles();

        assertEquals("a", "a");


    }

}
