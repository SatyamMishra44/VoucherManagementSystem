package com.example.Voucher.serviceImpl;


import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.entity.VoucherRedemption;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.VoucherRedemptionRepository;
import com.example.Voucher.repository.VoucherRepository;
import com.example.Voucher.service.VoucherRedemptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@Transactional
public class VoucherRedemptionServiceImpl implements VoucherRedemptionService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BillRepository billRepository;


    // Constructor injection to inject the bean
    public VoucherRedemptionServiceImpl(
            VoucherRepository voucherRepository,
            VoucherRedemptionRepository voucherRedemptionRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            BillRepository billRepository
    ){
        this.voucherRepository = voucherRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.billRepository = billRepository;
    }
    @Override
    public Transaction redeemVoucher(Long userId, String voucherCode, Long billId) {
        // Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"));

        // Fetch bill and enforce ownership from DB (never trust client amount).
        Bill bill = billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Fetch voucher scoped to assigned user and lock row for race-safe redemption.
        Voucher voucher = voucherRepository.findByCodeAndAssignedUserIdForUpdate(voucherCode, userId)
                .orElseThrow(()-> new RuntimeException("Voucher not found"));


        // Validate eligibility rules before redemption.
        if(!voucher.isEnabled()){
            throw new RuntimeException("voucher is disabled");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(voucher.getStartDate()) || today.isAfter(voucher.getExpiryDate())) {
            throw new RuntimeException("voucher is not valid on this date");
        }

        if (bill.getTotalAmount().compareTo(voucher.getMinBillAmount()) < 0) {
            throw new RuntimeException("bill amount is below the voucher minimum");
        }

        if (voucher.isRedeemed()) {
            throw new RuntimeException("Voucher already redeemed");
        }

        BigDecimal totalAmount = bill.getTotalAmount();
        BigDecimal discountRate = BigDecimal.valueOf(voucher.getDiscountPercentage())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal discount = totalAmount.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        // 7️⃣ Create transaction
        Transaction transaction =
                new Transaction(user, bill, totalAmount, finalAmount);
        transactionRepository.save(transaction);

        // 8️⃣ Create redemption audit record
        VoucherRedemption redemption =
                new VoucherRedemption(user, voucher, transaction,discount);
        voucherRedemptionRepository.save(redemption);

        voucher.markRedeemed(bill);
        voucherRepository.save(voucher);

        return transaction;

    }

    @Override
    public VoucherRedemption getRedemptionById(Long redemptionId) {

        return voucherRedemptionRepository.findById(redemptionId)
                .orElseThrow(()-> new RuntimeException("Redemption not found"));
    }

    @Override
    public java.util.List<VoucherRedemption> getRedemptionsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return voucherRedemptionRepository.findByUserIdOrderByRedeemedAtDesc(userId);
    }

    @Override
    public java.util.List<VoucherRedemption> getRedemptionsByVoucherId(Long voucherId) {
        if (voucherId == null) {
            throw new IllegalArgumentException("Voucher ID cannot be null");
        }
        return voucherRedemptionRepository.findByVoucherIdOrderByRedeemedAtDesc(voucherId);
    }
}
