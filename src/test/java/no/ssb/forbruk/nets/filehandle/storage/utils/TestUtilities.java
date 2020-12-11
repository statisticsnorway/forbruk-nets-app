package no.ssb.forbruk.nets.filehandle.storage.utils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtilities {

    public static ChannelSftp.LsEntry lsEntryWithGivenFilename(String filename) {
        ChannelSftp.LsEntry lsEntry = mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs = mock(SftpATTRS.class);
        when(lsEntry.getAttrs()).thenReturn(attrs);
        when(lsEntry.getFilename()).thenReturn(filename);
        return lsEntry;
    }
}
