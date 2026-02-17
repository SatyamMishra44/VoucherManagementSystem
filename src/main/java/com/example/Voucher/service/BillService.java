package com.example.Voucher.service;

import com.example.Voucher.entity.Bill;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BillService {

    //create a bill with total amount
    Bill createBill(Bill bill);

    //get bill by billId
    // Optional will force u to handle the NullPointerException
    Optional<Bill> getBillById(Long billId);

    Optional<Bill> getBillByIdForUser(Long billId, Long userId);

    List<Bill> getBillsByUserId(Long userId);


    //calculate total bill amount before applying the voucher
    BigDecimal calculateTotalAmount(Long billId);
}
