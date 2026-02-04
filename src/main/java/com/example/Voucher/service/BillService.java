package com.example.Voucher.service;

import com.example.Voucher.entity.Bill;

import java.util.Optional;

public interface BillService {

    //create a bill with total amount
    Bill createBill(Bill bill);

    //get bill by billId
    // Optional will force u to handle the NullPointerException
    Optional<Bill> getBillById(Long billId);


    //calculate total bill amount before applying the voucher
    Double calculateTotalAmount(Long billId);
}
