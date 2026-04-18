package com.jay.LetsSplitIt.Entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "group_id")
    private UUID groupId;

    @NonNull
    @Column(name = "paid_by", nullable = false)
    private UUID paidBy;

    @NonNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "expense_participants",
            joinColumns = @JoinColumn(name = "expense_id")
    )
    @Column(name = "user_id")
    private List<UUID> splitBetween;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum SplitType {
        EQUAL, EXACT, PERCENTAGE, SHARES
    }
}
