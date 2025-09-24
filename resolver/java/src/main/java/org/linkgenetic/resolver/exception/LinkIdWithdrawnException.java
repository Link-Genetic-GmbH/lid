package org.linkgenetic.resolver.exception;

import org.linkgenetic.resolver.model.Tombstone;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class LinkIdWithdrawnException extends RuntimeException {

    private final String linkId;
    private final Tombstone tombstone;

    public LinkIdWithdrawnException(String linkId, Tombstone tombstone) {
        super("LinkID withdrawn: " + linkId);
        this.linkId = linkId;
        this.tombstone = tombstone;
    }

    public LinkIdWithdrawnException(String linkId, Tombstone tombstone, String message) {
        super(message);
        this.linkId = linkId;
        this.tombstone = tombstone;
    }

    public String getLinkId() {
        return linkId;
    }

    public Tombstone getTombstone() {
        return tombstone;
    }
}