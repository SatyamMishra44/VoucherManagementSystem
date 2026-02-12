package com.example.Voucher.serviceImpl;


import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.Voucher;
import com.example.Voucher.entity.VoucherRedemption;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.VoucherRedemptionRepository;
import com.example.Voucher.repository.VoucherRepository;
import com.example.Voucher.service.VoucherRedemptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class VoucherRedemptionServiceImpl implements VoucherRedemptionService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;


    // Constructor injection to inject the bean
    public VoucherRedemptionServiceImpl(
            VoucherRepository voucherRepository,
            VoucherRedemptionRepository voucherRedemptionRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ){
        this.voucherRepository = voucherRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }
    @Override
    public Transaction redeemVoucher(Long userId, String voucherCode, Double billAmount) {
        // Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found"));

        // fetch the voucher by their voucher code
        Voucher voucher = voucherRepository.findByCode(voucherCode)
                .orElseThrow(()-> new RuntimeException("Voucher not found"));


        // Validate eligibility rules before redemption.
        if(!voucher.isEnabled()){
            throw new RuntimeException("voucher is disabled");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(voucher.getStartDate()) || today.isAfter(voucher.getExpiryDate())) {
            throw new RuntimeException("voucher is not valid on this date");
        }

        if (billAmount < voucher.getMinBillAmount()) {
            throw new RuntimeException("bill amount is below the voucher minimum");
        }

        if (voucher.hasExceededUsageLimit()){
            throw new RuntimeException("Voucher usage limit exceeded");
        }

        boolean alreadyRedeemed = voucherRedemptionRepository
                .existsByVoucherIdAndUserId(voucher.getId(), user.getId());
        if (alreadyRedeemed) {
            throw new RuntimeException("Voucher already redeemed by this user");
        }

        // 5️⃣ Convert bill amount to smallest currency unit
        int totalAmount = billAmount.intValue();

        // 6️⃣ Apply percentage discount
        int discount =
                (int) ((totalAmount * voucher.getDiscountPercentage()) / 100);

        int finalAmount = totalAmount - discount;

        // 7️⃣ Create transaction
        Transaction transaction =
                new Transaction(user, totalAmount, finalAmount);
        transactionRepository.save(transaction);

        // 8️⃣ Create redemption audit record
        VoucherRedemption redemption =
                new VoucherRedemption(user, voucher, transaction,discount);
        voucherRedemptionRepository.save(redemption);

        // Atomic increment prevents concurrent over-redemption.
        int updated = voucherRepository.incrementUsageIfAvailable(voucher.getId());
        if (updated == 0) {
            throw new RuntimeException("Voucher usage limit exceeded");
        }

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
