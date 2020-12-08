package no.ssb.forbruk.nets.health;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.filehandle.sftp.SftpFileTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadinessService {
    private static final Logger logger = LoggerFactory.getLogger(ReadinessService.class);

    @NonNull
    final SftpFileTransfer sftpFileTransfer;


    public boolean isReady(){
        // This might not be best practice, since no traffic gets routed to the service, but it depends on
        // your service. This might lead to cascading errors in other services, and one should consider
        // queueing requests.
        return sftpFileTransfer.secretsOk();
    }

}
