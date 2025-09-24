package org.linkgenetic.resolver.service;

import org.linkgenetic.resolver.exception.LinkIdNotFoundException;
import org.linkgenetic.resolver.model.*;
import org.linkgenetic.resolver.repository.LinkIdRepository;
import org.linkgenetic.resolver.util.LinkIdGenerator;
import org.linkgenetic.resolver.util.LinkIdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(RegistryService.class);

    private final LinkIdRepository linkIdRepository;
    private final LinkIdGenerator generator;
    private final LinkIdValidator validator;
    private final CacheService cacheService;

    public RegistryService(LinkIdRepository linkIdRepository,
                          LinkIdGenerator generator,
                          LinkIdValidator validator,
                          CacheService cacheService) {
        this.linkIdRepository = linkIdRepository;
        this.generator = generator;
        this.validator = validator;
        this.cacheService = cacheService;
    }

    public RegistrationResponse register(RegistrationRequest request) {
        String id = generator.generateUUID();
        Instant now = Instant.now();

        ResolutionRecord record = new ResolutionRecord(
                request.getTargetUri(),
                "active",
                request.getMediaType() != null ? request.getMediaType() : "text/html"
        );
        record.setLanguage(request.getLanguage() != null ? request.getLanguage() : "en");
        record.setQuality(1.0);
        record.setValidFrom(now);
        record.setLastModified(now);
        record.setMetadata(request.getMetadata());

        LinkIdRecord linkIdRecord = new LinkIdRecord(id, "active", now, request.getIssuer());
        linkIdRecord.setRecords(Collections.singletonList(record));

        ResolutionPolicy policy = new ResolutionPolicy();
        policy.setCacheTTL(3600);
        linkIdRecord.setPolicy(policy);

        linkIdRepository.save(linkIdRecord);

        logger.info("Registered new LinkID: {} for URI: {}", id, request.getTargetUri());

        return new RegistrationResponse(id, "active", now, "https://w3id.org/linkid/" + id);
    }

    public LinkIdRecord update(String id, List<ResolutionRecord> newRecords, String issuer) {
        validator.validate(id);
        String normalizedId = validator.normalize(id);

        LinkIdRecord existing = linkIdRepository.findByIdAndStatus(normalizedId, "active")
                .orElseThrow(() -> new LinkIdNotFoundException(normalizedId));

        if (!issuer.equals(existing.getIssuer())) {
            throw new SecurityException("Not authorized to update this LinkID");
        }

        existing.setRecords(newRecords);
        existing.setUpdated(Instant.now());

        LinkIdRecord updated = linkIdRepository.save(existing);

        cacheService.evictPattern(normalizedId + "*");

        logger.info("Updated LinkID: {} with {} records", normalizedId, newRecords.size());

        return updated;
    }

    public void withdraw(String id, String reason, String contact, String issuer) {
        validator.validate(id);
        String normalizedId = validator.normalize(id);

        LinkIdRecord existing = linkIdRepository.findByIdAndStatus(normalizedId, "active")
                .orElseThrow(() -> new LinkIdNotFoundException(normalizedId));

        if (!issuer.equals(existing.getIssuer())) {
            throw new SecurityException("Not authorized to withdraw this LinkID");
        }

        Tombstone tombstone = new Tombstone(Instant.now(), reason, contact);
        existing.setStatus("withdrawn");
        existing.setTombstone(tombstone);
        existing.setUpdated(Instant.now());

        linkIdRepository.save(existing);

        cacheService.evictPattern(normalizedId + "*");

        logger.info("Withdrew LinkID: {} with reason: {}", normalizedId, reason);
    }

    public LinkIdRecord get(String id) {
        validator.validate(id);
        String normalizedId = validator.normalize(id);

        return linkIdRepository.findById(normalizedId)
                .orElseThrow(() -> new LinkIdNotFoundException(normalizedId));
    }

    public List<LinkIdRecord> getByIssuer(String issuer) {
        return linkIdRepository.findByIssuer(issuer);
    }

    public List<LinkIdRecord> getByStatus(String status) {
        return linkIdRepository.findByStatus(status);
    }

    public boolean exists(String id) {
        if (!validator.isValid(id)) {
            return false;
        }
        String normalizedId = validator.normalize(id);
        return linkIdRepository.existsById(normalizedId);
    }

    public long countByStatus(String status) {
        return linkIdRepository.countByStatus(status);
    }

    public long countByIssuer(String issuer) {
        return linkIdRepository.countByIssuer(issuer);
    }

    public RegistryStats getStats() {
        long activeCount = countByStatus("active");
        long withdrawnCount = countByStatus("withdrawn");
        long totalCount = activeCount + withdrawnCount;

        return new RegistryStats(totalCount, activeCount, withdrawnCount);
    }

    public static class RegistryStats {
        private final Long total;
        private final Long active;
        private final Long withdrawn;

        public RegistryStats(Long total, Long active, Long withdrawn) {
            this.total = total;
            this.active = active;
            this.withdrawn = withdrawn;
        }

        public Long getTotal() {
            return total;
        }

        public Long getActive() {
            return active;
        }

        public Long getWithdrawn() {
            return withdrawn;
        }
    }
}