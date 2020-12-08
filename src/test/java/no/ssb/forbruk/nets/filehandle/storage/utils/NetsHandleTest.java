package no.ssb.forbruk.nets.filehandle.storage.utils;

import no.ssb.forbruk.nets.db.repository.NetsRecordRepository;
import no.ssb.forbruk.nets.filehandle.NetsHandle;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import no.ssb.forbruk.nets.filehandle.storage.GoogleCloudStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

//@ExtendWith(MockitoExtension.class)
public class NetsHandleTest {

    @Mock
    private SftpFileTransfer fileTransfer;

    @Mock
    private GoogleCloudStorage googleCloudStorage;

    @Mock
    private NetsRecordRepository netsRecordRepository;

    @InjectMocks
    private NetsHandle netsHandle;

//    @Test
//    public testTest() {
//        // InputStream stream = new InputStream();
//
//        // given
//        // given(fileTransfer.getFileInputStream(any()))
//        //        .willReturn(stream);
//
//        // when
//        netsHandle.getAndHandleNetsFiles();
//
//        // then
//
//    }
}
