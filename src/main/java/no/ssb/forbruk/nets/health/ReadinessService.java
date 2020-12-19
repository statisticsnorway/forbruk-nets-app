package no.ssb.forbruk.nets.health;

import no.ssb.forbruk.nets.sftp.SftpFileTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReadinessService {

    private SftpFileTransfer sftpFileTransfer;

    @Autowired
    public ReadinessService(SftpFileTransfer sftpFileTransfer){this.sftpFileTransfer = sftpFileTransfer;}

    private static final Logger logger = LoggerFactory.getLogger(ReadinessService.class);

    public boolean isReady(){
        // This might not be best practice, since no traffic gets routed to the service, but it depends on
        // your service. This might lead to cascading errors in other services, and one should consider
        // queueing requests.
        return sftpFileTransfer.secretsOk();
    }

}
