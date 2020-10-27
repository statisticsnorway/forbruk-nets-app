package no.ssb.forbruk.nets.scheduling;

import no.ssb.forbruk.nets.sftp.SftpFileTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    SftpFileTransfer sftpFileTransfer;

    @Scheduled(cron = "${scheduled.cron.listfiles}")
    public void runSftpFileTransferList() {
        logger.info("schedule task - " + System.currentTimeMillis() / 1000);
        sftpFileTransfer.list();
    }
}
