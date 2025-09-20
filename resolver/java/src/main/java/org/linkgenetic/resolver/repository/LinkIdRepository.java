package org.linkgenetic.resolver.repository;

import org.linkgenetic.resolver.model.LinkIdRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkIdRepository extends MongoRepository<LinkIdRecord, String> {

    Optional<LinkIdRecord> findByIdAndStatus(String id, String status);

    List<LinkIdRecord> findByIssuer(String issuer);

    List<LinkIdRecord> findByStatus(String status);

    @Query("{ 'created': { $gte: ?0, $lte: ?1 } }")
    List<LinkIdRecord> findByCreatedBetween(Instant start, Instant end);

    @Query("{ 'status': 'active', 'records': { $elemMatch: { 'status': 'active' } } }")
    List<LinkIdRecord> findActiveRecordsWithActiveResolutions();

    long countByStatus(String status);

    long countByIssuer(String issuer);

    @Query(value = "{ 'id': ?0 }", exists = true)
    boolean existsById(String id);

    void deleteByIdAndIssuer(String id, String issuer);
}