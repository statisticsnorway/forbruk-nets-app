package no.ssb.forbruk.nets.filehandle.sftp;

import com.google.inject.Injector;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.micrometer.core.instrument.MeterRegistry;
import no.ssb.forbruk.nets.filehandle.storage.utils.TestUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class SftpFileTransferTest {

    ChannelSftp.LsEntry testfile1 = TestUtilities.lsEntryWithGivenFilename("test1.csv");
    ChannelSftp.LsEntry testfile2 = TestUtilities.lsEntryWithGivenFilename("test2.csv");
    ChannelSftp.LsEntry testfile3 = TestUtilities.lsEntryWithGivenFilename("test3.csv");

    @Mock
    MeterRegistry meterRegistry;
    @Mock
    ChannelSftp channelSftp = new ChannelSftp();

    @InjectMocks
    SftpFileTransfer sftpFileTransfer ;


//    @Test
    public void fileListTest_oneFileOk() throws SftpException {
        Vector<ChannelSftp.LsEntry> testFiles = new Vector(Arrays.asList(new ChannelSftp.LsEntry[]{testfile1}));
        when(channelSftp.ls(anyString())).thenReturn(testFiles);
        Collection<ChannelSftp.LsEntry> resultFiles = sftpFileTransfer.fileList();
        assertEquals(1, resultFiles.size());
        assertEquals("test1.csv", resultFiles.iterator().next().getFilename());
    }

//    @Test
    public void fileListTest_threeFilesOk() throws SftpException {
        Vector<ChannelSftp.LsEntry> testFiles = new Vector(Arrays.asList(new ChannelSftp.LsEntry[]{testfile1, testfile2, testfile3}));
        when(channelSftp.ls(anyString())).thenReturn(testFiles);
        Collection<ChannelSftp.LsEntry> resultFiles = sftpFileTransfer.fileList();
        assertEquals(3, resultFiles.size());
        assertEquals("test1.csv", resultFiles.iterator().next().getFilename());
    }
}
