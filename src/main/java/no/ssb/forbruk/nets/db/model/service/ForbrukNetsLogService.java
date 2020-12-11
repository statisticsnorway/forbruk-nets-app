package no.ssb.forbruk.nets.db.model.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import no.ssb.forbruk.nets.db.model.ForbrukNetsLog;
import no.ssb.forbruk.nets.db.repository.ForbrukNetsLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForbrukNetsLogService {

    @NonNull
    final ForbrukNetsLogRepository forbrukNetsLogRepository;

    public void saveLogOK(String filename, String counted, Long number) {
        saveLog(filename, counted, number, "OK");
    }

    public void saveLogError(String filename, String counted, Long number) {
        saveLog(filename, counted, number, "ERROR");
    }

    public void saveLog(String filename, String counted, Long number, String status) {
        forbrukNetsLogRepository.save(
                ForbrukNetsLog.builder()
                        .filename(filename)
                        .counted(counted)
                        .number(number)
                        .status(status)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

}
