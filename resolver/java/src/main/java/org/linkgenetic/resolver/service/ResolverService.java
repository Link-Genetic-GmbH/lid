package org.linkgenetic.resolver.service;

import org.linkgenetic.resolver.exception.InvalidLinkIdFormatException;
import org.linkgenetic.resolver.exception.LinkIdNotFoundException;
import org.linkgenetic.resolver.exception.LinkIdWithdrawnException;
import org.linkgenetic.resolver.model.*;
import org.linkgenetic.resolver.repository.LinkIdRepository;
import org.linkgenetic.resolver.util.LinkIdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResolverService {

    private static final Logger logger = LoggerFactory.getLogger(ResolverService.class);

    private final LinkIdRepository linkIdRepository;
    private final CacheService cacheService;
    private final LinkIdValidator validator;

    public ResolverService(LinkIdRepository linkIdRepository,
                          CacheService cacheService,
                          LinkIdValidator validator) {
        this.linkIdRepository = linkIdRepository;
        this.cacheService = cacheService;
        this.validator = validator;
    }

    public ResolutionResult resolve(String id, Map<String, String> requestParams) {
        validator.validate(id);
        String normalizedId = validator.normalize(id);

        String cacheKey = generateCacheKey(normalizedId, requestParams);
        Optional<ResolutionResult> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            logger.debug("Cache hit for LinkID: {}", normalizedId);
            return cached.get();
        }

        LinkIdRecord record = linkIdRepository.findByIdAndStatus(normalizedId, "active")
                .orElseThrow(() -> new LinkIdNotFoundException(normalizedId));

        if ("withdrawn".equals(record.getStatus())) {
            throw new LinkIdWithdrawnException(normalizedId, record.getTombstone());
        }

        List<ResolutionRecord> candidates = filterCandidates(record.getRecords(), requestParams);
        if (candidates.isEmpty()) {
            throw new LinkIdNotFoundException(normalizedId, "No matching records found");
        }

        List<ResolutionRecord> rankedCandidates = rankCandidates(candidates, requestParams);
        ResolutionResult result = createResult(record, rankedCandidates, requestParams);

        Integer cacheTTL = record.getPolicy() != null ? record.getPolicy().getCacheTTL() : 3600;
        cacheService.put(cacheKey, result, cacheTTL);

        return result;
    }

    private List<ResolutionRecord> filterCandidates(List<ResolutionRecord> records, Map<String, String> params) {
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream()
                .filter(record -> "active".equals(record.getStatus()))
                .filter(record -> isValidRecord(record))
                .filter(record -> matchesFormat(record, params.get("format")))
                .filter(record -> matchesLanguage(record, params.get("lang")))
                .collect(Collectors.toList());
    }

    private boolean isValidRecord(ResolutionRecord record) {
        Instant now = Instant.now();

        if (record.getValidFrom() != null && record.getValidFrom().isAfter(now)) {
            return false;
        }

        if (record.getValidUntil() != null && record.getValidUntil().isBefore(now)) {
            return false;
        }

        return true;
    }

    private boolean matchesFormat(ResolutionRecord record, String requestedFormat) {
        if (requestedFormat == null) {
            return true;
        }
        return requestedFormat.equalsIgnoreCase(record.getMediaType());
    }

    private boolean matchesLanguage(ResolutionRecord record, String requestedLanguage) {
        if (requestedLanguage == null) {
            return true;
        }
        return requestedLanguage.equalsIgnoreCase(record.getLanguage());
    }

    private List<ResolutionRecord> rankCandidates(List<ResolutionRecord> candidates, Map<String, String> params) {
        return candidates.stream()
                .sorted((a, b) -> {
                    int qualityComparison = Double.compare(
                            b.getQuality() != null ? b.getQuality() : 0.0,
                            a.getQuality() != null ? a.getQuality() : 0.0
                    );

                    if (qualityComparison != 0) {
                        return qualityComparison;
                    }

                    Instant aModified = a.getLastModified();
                    Instant bModified = b.getLastModified();
                    if (aModified != null && bModified != null) {
                        return bModified.compareTo(aModified);
                    }

                    return 0;
                })
                .collect(Collectors.toList());
    }

    private ResolutionResult createResult(LinkIdRecord record, List<ResolutionRecord> candidates, Map<String, String> params) {
        boolean preferRedirect = !"metadata".equals(params.get("format"));

        if (preferRedirect) {
            ResolutionRecord best = candidates.get(0);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "max-age=" + (record.getPolicy() != null ? record.getPolicy().getCacheTTL() : 3600));
            headers.put("X-LinkID-Quality", String.valueOf(best.getQuality()));
            headers.put("X-LinkID-Resolver", "linkid-resolver-java");

            return new ResolutionResult.RedirectResult(best.getUri(), best.getQuality(), headers);
        } else {
            return new ResolutionResult.MetadataResult(record);
        }
    }

    private String generateCacheKey(String id, Map<String, String> params) {
        StringBuilder key = new StringBuilder(id);
        if (params != null && !params.isEmpty()) {
            key.append(":");
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> key.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
        }
        return key.toString();
    }
}