package com.transaction.gateway.repository;

import com.transaction.gateway.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    // JpaRepository already gives us save(), findById(), findAll(), etc.
    // We'll add custom query methods here later if needed
    // (e.g. findByMerchantIdAndTimestampBetween for reporting)
}