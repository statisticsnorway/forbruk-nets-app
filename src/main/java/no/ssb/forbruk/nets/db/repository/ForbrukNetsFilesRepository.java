package no.ssb.forbruk.nets.db.repository;

import no.ssb.forbruk.nets.db.model.ForbrukNetsFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
@Repository
//@RepositoryRestResource(collectionResourceRel = "netsRecord", path = "netsRecord")
public interface ForbrukNetsFilesRepository extends JpaRepository<ForbrukNetsFiles, Long> {


}
