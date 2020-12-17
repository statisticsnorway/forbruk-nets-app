package no.ssb.forbruk.nets.db.model;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
@Entity
@Builder
public class ForbrukNetsFiles {

    private static final String ID_COLUMN = "NETS_REC_ID";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_" + ID_COLUMN)
    @SequenceGenerator(name = "SEQ_" + ID_COLUMN, sequenceName = "SEQ_" + ID_COLUMN, allocationSize = 1)
//    @NonNull
    private Long id;
    @NonNull
    private LocalDateTime timestamp;
    @NonNull
    private String filename;
    @NonNull
    private Long transactions;
}

