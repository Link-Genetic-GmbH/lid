package org.linkgenetic.resolver.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Component
public class LinkIdGenerator {

    private final SecureRandom secureRandom;

    public LinkIdGenerator() {
        this.secureRandom = new SecureRandom();
    }

    public String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "").toLowerCase();
    }

    public String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String generateTimestampBasedId() {
        long timestamp = Instant.now().toEpochMilli();
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        String timestampHex = Long.toHexString(timestamp);
        String randomHex = bytesToHex(randomBytes);

        return (timestampHex + randomHex).toLowerCase();
    }

    public String generateSecureRandom(int length) {
        if (length < 32 || length > 64) {
            throw new IllegalArgumentException("Length must be between 32 and 64");
        }

        byte[] randomBytes = new byte[length / 2];
        secureRandom.nextBytes(randomBytes);
        return bytesToHex(randomBytes).toLowerCase();
    }

    public String generateFromContent(String uri, String content) {
        String combined = uri + "|" + content + "|" + Instant.now().toString();
        return generateHash(combined).substring(0, 32);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}