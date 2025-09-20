package org.linkgenetic.resolver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LinkIdNotFoundException extends RuntimeException {

    private final String linkId;

    public LinkIdNotFoundException(String linkId) {
        super("LinkID not found: " + linkId);
        this.linkId = linkId;
    }

    public LinkIdNotFoundException(String linkId, String message) {
        super(message);
        this.linkId = linkId;
    }

    public String getLinkId() {
        return linkId;
    }
}