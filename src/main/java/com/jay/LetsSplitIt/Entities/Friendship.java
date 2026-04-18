package com.jay.LetsSplitIt.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"request_sent_by", "request_sent_to"}))
public class Friendship {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "request_sent_by")
    private UUID sentBy;

    @Column(name = "request_sent_to")
    private UUID sentTo;

    public enum Status{
        ACCEPTED,
        PENDING,
        BLOCKED
    }

    private Status status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
