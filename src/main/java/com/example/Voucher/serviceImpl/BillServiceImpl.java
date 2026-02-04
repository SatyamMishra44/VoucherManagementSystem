package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.service.BillService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillServiceImpl implements BillService {
    private final BillRepository billRepository;

    public BillServiceImpl(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    /**
     * Create and persist a bill
     */
    @Override
    public Bill createBill(Bill bill) {



        if (bill == null) {
            throw new IllegalArgumentException("Bill cannot be null");
        }

        if (bill.getUser() == null) {
            throw new IllegalArgumentException("Bill must be associated with a user");
        }

        if (bill.getTotalAmount() == null || bill.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("Bill amount must be greater than zero");
        }

        return billRepository.save(bill);
    }

    /**
     * Fetch bill by id
     */
    @Override
    public Optional<Bill> getBillById(Long billId) {

        if (billId == null) {
            throw new IllegalArgumentException("Bill ID cannot be null");
        }

        return billRepository.findById(billId);
    }

    /**
     * Fetch all bills of a user
     */
    @Override
    public List<Bill> getBillsByUserId(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return billRepository.findByUserId(userId);
    }




    @Override
    public Double calculateTotalAmount(Long billId) {
        return 0.0;
    }
}
