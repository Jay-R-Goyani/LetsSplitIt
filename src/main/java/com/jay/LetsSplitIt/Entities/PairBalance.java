package com.jay.LetsSplitIt.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pair_balances", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"debtor_id", "creditor_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairBalance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "debtor_id", nullable = false)
    private UUID debtorId;

    @Column(name = "creditor_id", nullable = false)
    private UUID creditorId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Version
    private Long version;
}
