package org.linkgenetic.resolver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "linkid_records")
public class LinkIdRecord {

    @Id
    private String id;

    @Indexed
    private String status;

    @Indexed
    private Instant created;

    private Instant updated;

    @Indexed
    private String issuer;

    private List<ResolutionRecord> records;

    private List<AlternateIdentifier> alternates;

    private ResolutionPolicy policy;

    private Tombstone tombstone;

    private Map<String, Object> metadata;

    public LinkIdRecord() {}

    public LinkIdRecord(String id, String status, Instant created, String issuer) {
        this.id = id;
        this.status = status;
        this.created = created;
        this.updated = created;
        this.issuer = issuer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public List<ResolutionRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ResolutionRecord> records) {
        this.records = records;
    }

    public List<AlternateIdentifier> getAlternates() {
        return alternates;
    }

    public void setAlternates(List<AlternateIdentifier> alternates) {
        this.alternates = alternates;
    }

    public ResolutionPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(ResolutionPolicy policy) {
        this.policy = policy;
    }

    public Tombstone getTombstone() {
        return tombstone;
    }

    public void setTombstone(Tombstone tombstone) {
        this.tombstone = tombstone;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class AlternateIdentifier {
        private String scheme;
        private String identifier;

        public AlternateIdentifier() {}

        public AlternateIdentifier(String scheme, String identifier) {
            this.scheme = scheme;
            this.identifier = identifier;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
    }
}