package com.jay.LetsSplitIt.Repository;

import com.jay.LetsSplitIt.Entities.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    @Query("""
            SELECT s FROM Settlement s
            WHERE s.payerId = :userId OR s.receiverId = :userId
            ORDER BY s.createdAt DESC
            """)
    Page<Settlement> findActivityForUser(@Param("userId") UUID userId, Pageable pageable);
}