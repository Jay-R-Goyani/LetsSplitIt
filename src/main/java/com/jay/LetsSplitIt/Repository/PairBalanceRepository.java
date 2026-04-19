package com.jay.LetsSplitIt.Repository;

import com.jay.LetsSplitIt.Entities.PairBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PairBalanceRepository extends JpaRepository<PairBalance, UUID> {

    Optional<PairBalance> findByDebtorIdAndCreditorId(UUID debtorId, UUID creditorId);

    List<PairBalance> findByDebtorId(UUID debtorId);

    List<PairBalance> findByCreditorId(UUID creditorId);
}
