package org.linkgenetic.resolver.model;

import java.time.Instant;

public class Tombstone {

    private Instant withdrawnAt;
    private String reason;
    private String contact;
    private String replacedBy;

    public Tombstone() {}

    public Tombstone(Instant withdrawnAt, String reason, String contact) {
        this.withdrawnAt = withdrawnAt;
        this.reason = reason;
        this.contact = contact;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }
}