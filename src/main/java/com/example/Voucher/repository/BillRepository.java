package com.example.Voucher.repository;


import com.example.Voucher.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserId(Long userId); // this will us to get the all the bill details that belongs to the specific user by findByUserId method.

}
