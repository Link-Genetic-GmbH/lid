package org.linkgenetic.resolver.util;

import org.linkgenetic.resolver.exception.InvalidLinkIdFormatException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LinkIdValidator {

    private static final Pattern LINKID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{32,64}$");
    private static final int MIN_LENGTH = 32;
    private static final int MAX_LENGTH = 64;

    public boolean isValid(String linkId) {
        if (linkId == null || linkId.trim().isEmpty()) {
            return false;
        }

        String trimmed = linkId.trim().toLowerCase();

        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            return false;
        }

        return LINKID_PATTERN.matcher(trimmed).matches();
    }

    public void validate(String linkId) throws InvalidLinkIdFormatException {
        if (!isValid(linkId)) {
            throw new InvalidLinkIdFormatException(linkId,
                "LinkID must be 32-64 alphanumeric characters");
        }
    }

    public String normalize(String linkId) {
        if (linkId == null) {
            return null;
        }
        return linkId.trim().toLowerCase();
    }

    public boolean isUUID(String linkId) {
        if (!isValid(linkId)) {
            return false;
        }

        String normalized = normalize(linkId);
        return normalized.length() == 32 &&
               normalized.matches("^[a-f0-9]{32}$");
    }

    public boolean isHash(String linkId) {
        if (!isValid(linkId)) {
            return false;
        }

        String normalized = normalize(linkId);
        return (normalized.length() == 32 || normalized.length() == 64) &&
               normalized.matches("^[a-f0-9]+$");
    }
}