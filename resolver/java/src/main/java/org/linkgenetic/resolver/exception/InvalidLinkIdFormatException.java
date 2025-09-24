package org.linkgenetic.resolver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidLinkIdFormatException extends RuntimeException {

    private final String linkId;

    public InvalidLinkIdFormatException(String linkId) {
        super("Invalid LinkID format: " + linkId);
        this.linkId = linkId;
    }

    public InvalidLinkIdFormatException(String linkId, String message) {
        super(message);
        this.linkId = linkId;
    }

    public String getLinkId() {
        return linkId;
    }
}