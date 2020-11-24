package no.ssb.forbruk.nets.db.repository;

import no.ssb.forbruk.nets.db.model.NetsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.repository.CrudRepository;
//import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin
//@RepositoryRestResource(collectionResourceRel = "netsRecord", path = "netsRecord")
public interface NetsRecordRepository extends JpaRepository<NetsRecord, Long> {

}
