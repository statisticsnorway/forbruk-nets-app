package no.ssb.forbruk.nets.filehandle;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class NetsHandleTest {

    @Mock
    private SftpFileTransfer sftpFileTransfer;

    @Mock
    private GoogleCloudStorage googleCloudStorage;

    @Mock
    private NetsRecordRepository netsRecordRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private NetsHandle netsHandle;

    @Test
    public void testTest() throws IOException, SftpException, JSchException {
        InputStream inputStream = new FileInputStream(new File("src/test/resources/testNetsResponse.csv"));
        ChannelSftp.LsEntry testfile = lsEntryWithGivenFilename("test.csv");
        Collection<ChannelSftp.LsEntry> fileList = new Vector<ChannelSftp.LsEntry>();
        fileList.add(testfile);

        when(sftpFileTransfer.setupJsch()).thenReturn(true);
        when(sftpFileTransfer.fileList()).thenReturn(fileList);
        when(sftpFileTransfer.getFileInputStream(any())).thenReturn(inputStream);
        when(googleCloudStorage.produceMessages(any(), anyString())).thenReturn(5);


        netsHandle.getAndHandleNetsFiles();

        assertEquals("a", "a");


    }


    private ChannelSftp.LsEntry lsEntryWithGivenFilename(String filename) {
        ChannelSftp.LsEntry lsEntry = mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs = mock(SftpATTRS.class);
        when(lsEntry.getAttrs()).thenReturn(attrs);
        when(lsEntry.getFilename()).thenReturn(filename);
        return lsEntry;
    }

}
