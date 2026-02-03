package com.example.Voucher.repository;

import com.example.Voucher.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository //enable spring bean creation and talk to the database
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    List<Transaction> findByUserId(Long userId); // this will help us to get the all transaction that belongs to a specific user findByUserId
}
