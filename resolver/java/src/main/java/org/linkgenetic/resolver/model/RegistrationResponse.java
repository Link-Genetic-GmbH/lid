package org.linkgenetic.resolver.model;

import java.time.Instant;

public class RegistrationResponse {

    private String id;
    private String status;
    private Instant created;
    private String resolverUri;

    public RegistrationResponse() {}

    public RegistrationResponse(String id, String status, Instant created, String resolverUri) {
        this.id = id;
        this.status = status;
        this.created = created;
        this.resolverUri = resolverUri;
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

    public String getResolverUri() {
        return resolverUri;
    }

    public void setResolverUri(String resolverUri) {
        this.resolverUri = resolverUri;
    }
}