package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.repository.RedemptionHistoryRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.RedemptionResult;
import com.example.Voucher.service.UserVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class UserVoucherServiceImpl implements UserVoucherService {

    private final UserRepository userRepository;
    private final VoucherTemplateRepository voucherTemplateRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final RedemptionHistoryRepository redemptionHistoryRepository;
    private final BillRepository billRepository;
    private final TransactionRepository transactionRepository;

    public UserVoucherServiceImpl(UserRepository userRepository,
                                  VoucherTemplateRepository voucherTemplateRepository,
                                  UserVoucherRepository userVoucherRepository,
                                  RedemptionHistoryRepository redemptionHistoryRepository,
                                  BillRepository billRepository,
                                  TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.voucherTemplateRepository = voucherTemplateRepository;
        this.userVoucherRepository = userVoucherRepository;
        this.redemptionHistoryRepository = redemptionHistoryRepository;
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public UserVoucher purchaseVoucher(Long userId, String voucherCode, Integer quantity) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Voucher code is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        VoucherTemplate template = voucherTemplateRepository.findByCode(voucherCode)
                .orElseThrow(() -> new IllegalArgumentException("Voucher template not found"));

        validateTemplateEligibility(template);

        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal totalPurchasedAmount = template.getUnitValue()
                .multiply(qty)
                .setScale(2, RoundingMode.HALF_UP);

        UserVoucher userVoucher = new UserVoucher(
                template,
                user,
                quantity,
                totalPurchasedAmount,
                totalPurchasedAmount
        );
        return userVoucherRepository.save(userVoucher);
    }

    @Override
    public RedemptionResult redeemVoucher(Long userId, Long userVoucherId, Long billId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (userVoucherId == null) {
            throw new IllegalArgumentException("User voucher id is required");
        }
        if (billId == null) {
            throw new IllegalArgumentException("Bill id is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserVoucher userVoucher = userVoucherRepository.findByIdAndUserIdForUpdate(userVoucherId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User voucher not found"));

        if (!userVoucher.isActive()) {
            throw new IllegalArgumentException("User voucher is inactive");
        }

        validateTemplateEligibility(userVoucher.getVoucherTemplate());

        if (userVoucher.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("User voucher has no remaining balance");
        }

        Bill bill = billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));
        BigDecimal effectiveBillAmount = bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        BigDecimal deduction = effectiveBillAmount.min(userVoucher.getRemainingBalance())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = effectiveBillAmount.subtract(deduction)
                .setScale(2, RoundingMode.HALF_UP);

        userVoucher.applyRedemption(deduction);
        userVoucherRepository.save(userVoucher);

        RedemptionHistory history = new RedemptionHistory(
                userVoucher,
                bill,
                deduction,
                userVoucher.getRemainingBalance()
        );
        redemptionHistoryRepository.save(history);

        Transaction transaction = new Transaction(user, bill, effectiveBillAmount, payableAmount);
        transactionRepository.save(transaction);

        return new RedemptionResult(
                history.getId(),
                bill.getId(),
                userVoucher,
                effectiveBillAmount,
                deduction,
                payableAmount,
                history.getRedeemedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucher> getUserVouchers(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userVoucherRepository.findByUserIdOrderByPurchasedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionHistory> getUserRedemptionHistory(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return redemptionHistoryRepository.findByUserVoucherUserIdOrderByRedeemedAtDesc(userId);
    }

    private void validateTemplateEligibility(VoucherTemplate template) {
        if (!template.isEnabled()) {
            throw new IllegalArgumentException("Voucher template is disabled");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(template.getStartDate()) || today.isAfter(template.getExpiryDate())) {
            throw new IllegalArgumentException("Voucher template is not valid on this date");
        }
    }
}
