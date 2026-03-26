package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.RedemptionHistory;
import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.UserVoucher;
import com.example.Voucher.entity.UserVoucherStatus;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.repository.RedemptionHistoryRepository;
import com.example.Voucher.repository.TransactionRepository;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.repository.UserVoucherRepository;
import com.example.Voucher.repository.VoucherTemplateRepository;
import com.example.Voucher.service.RedemptionResult;
import com.example.Voucher.service.UserVoucherService;
import com.example.Voucher.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserVoucherServiceImpl implements UserVoucherService {

    private static final Logger log = LoggerFactory.getLogger(UserVoucherServiceImpl.class);

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
    public UserVoucher purchaseVoucher(Long userId, String voucherCode, Integer quantity, String requestId) {
        log.info("action=purchaseVoucher started | userId={} voucherCode={} quantity={} requestId={}", userId,
                voucherCode,
                quantity, requestId);
        Long tenantId = TenantContext.requireTenantId();

        if (requestId != null) {
            java.util.Optional<UserVoucher> existing = userVoucherRepository.findByRequestIdAndTenantId(requestId,
                    tenantId);
            if (existing.isPresent()) {
                log.info(
                        "action=purchaseVoucher idempotency | userId={} requestId={} reason=Duplicate request detected, returning existing voucher",
                        userId, requestId);
                return existing.get();
            }
        }

        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new IllegalArgumentException("Voucher code is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> {
                    log.warn("action=purchaseVoucher failed | userId={} reason=User not found", userId);
                    return new IllegalArgumentException("User not found");
                });

        VoucherTemplate template = voucherTemplateRepository.findByCodeAndTenantId(voucherCode, tenantId)
                .orElseThrow(() -> {
                    log.warn(
                            "action=purchaseVoucher failed | userId={} voucherCode={} reason=Voucher template not found",
                            userId, voucherCode);
                    return new IllegalArgumentException("Voucher template not found");
                });

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
                totalPurchasedAmount,
                requestId);
        UserVoucher saved = userVoucherRepository.save(userVoucher);
        log.info(
                "action=purchaseVoucher completed | userId={} voucherCode={} quantity={} totalAmount={} userVoucherId={}",
                userId, voucherCode, quantity, totalPurchasedAmount, saved.getId());
        return saved;
    }

    @Override
    public RedemptionResult redeemVoucher(Long userId, Long userVoucherId, Long billId, String requestId) {
        long startTime = System.currentTimeMillis();
        log.info("action=redeemVoucher started | userId={} userVoucherId={} billId={} requestId={}", userId,
                userVoucherId, billId, requestId);
        Long tenantId = TenantContext.requireTenantId();

        if (requestId != null) {
            java.util.Optional<RedemptionHistory> existing = redemptionHistoryRepository
                    .findByRequestIdAndTenantId(requestId, tenantId);
            if (existing.isPresent()) {
                RedemptionHistory h = existing.get();
                log.info(
                        "action=redeemVoucher idempotency | userId={} requestId={} reason=Duplicate request detected, returning existing redemption history",
                        userId, requestId);

                BigDecimal billAmount = h.getBill().getTotalAmount();
                BigDecimal deduction = h.getRedeemedAmount();
                BigDecimal payable = billAmount.subtract(deduction).setScale(2, RoundingMode.HALF_UP);

                return new RedemptionResult(
                        h.getId(),
                        h.getBill().getId(),
                        h.getUserVoucher(),
                        billAmount,
                        deduction,
                        payable,
                        h.getRedeemedAt());
            }
        }

        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (userVoucherId == null) {
            throw new IllegalArgumentException("User voucher id is required");
        }
        if (billId == null) {
            throw new IllegalArgumentException("Bill id is required");
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> {
                    log.warn("action=redeemVoucher failed | userId={} reason=User not found", userId);
                    return new IllegalArgumentException("User not found");
                });

        log.debug("action=redeemVoucher | acquiring pessimistic lock for userVoucherId={} userId={}", userVoucherId,
                userId);
        UserVoucher userVoucher = userVoucherRepository.findByIdAndUserIdForUpdate(userVoucherId, userId, tenantId)
                .orElseThrow(() -> {
                    log.warn("action=redeemVoucher failed | userId={} userVoucherId={} reason=User voucher not found",
                            userId, userVoucherId);
                    return new IllegalArgumentException("User voucher not found");
                });
        log.debug("action=redeemVoucher | lock acquired for userVoucherId={} elapsed={}ms", userVoucherId,
                System.currentTimeMillis() - startTime);

        if (!userVoucher.isActive()) {
            log.warn("action=redeemVoucher failed | userId={} userVoucherId={} reason=Voucher inactive", userId,
                    userVoucherId);
            throw new IllegalArgumentException("User voucher is inactive");
        }

        validateTemplateEligibility(userVoucher.getVoucherTemplate());

        if (userVoucher.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("action=redeemVoucher failed | userId={} userVoucherId={} reason=Zero remaining balance", userId,
                    userVoucherId);
            throw new IllegalArgumentException("User voucher has no remaining balance");
        }

        Bill bill = billRepository.findByIdAndUserIdAndTenantId(billId, userId, tenantId)
                .orElseThrow(() -> {
                    log.warn("action=redeemVoucher failed | userId={} billId={} reason=Bill not found", userId, billId);
                    return new IllegalArgumentException("Bill not found");
                });
        BigDecimal effectiveBillAmount = bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        BigDecimal deduction = effectiveBillAmount.min(userVoucher.getRemainingBalance())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = effectiveBillAmount.subtract(deduction)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug(
                "action=redeemVoucher | userId={} userVoucherId={} billAmount={} deduction={} payable={} balanceBefore={} balanceAfter={}",
                userId, userVoucherId, effectiveBillAmount, deduction, payableAmount,
                userVoucher.getRemainingBalance(), userVoucher.getRemainingBalance().subtract(deduction));

        userVoucher.applyRedemption(deduction);
        userVoucherRepository.save(userVoucher);

        RedemptionHistory history = new RedemptionHistory(
                userVoucher,
                bill,
                deduction,
                userVoucher.getRemainingBalance(),
                requestId);
        redemptionHistoryRepository.save(history);

        Transaction transaction = new Transaction(user, bill, effectiveBillAmount, payableAmount, requestId);
        transactionRepository.save(transaction);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info(
                "action=redeemVoucher completed | userId={} userVoucherId={} billId={} deduction={} payable={} remainingBalance={} elapsed={}ms",
                userId, userVoucherId, billId, deduction, payableAmount, userVoucher.getRemainingBalance(), elapsed);

        return new RedemptionResult(
                history.getId(),
                bill.getId(),
                userVoucher,
                effectiveBillAmount,
                deduction,
                payableAmount,
                history.getRedeemedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucher> getUserVouchers(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        log.debug("action=getUserVouchers | userId={}", userId);
        List<UserVoucher> vouchers = userVoucherRepository.findByUserIdAndTenantIdOrderByPurchasedAtDesc(
                userId,
                TenantContext.requireTenantId());
        log.info("action=getUserVouchers completed | userId={} resultCount={}", userId, vouchers.size());
        return vouchers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionHistory> getUserRedemptionHistory(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        log.debug("action=getUserRedemptionHistory | userId={}", userId);
        List<RedemptionHistory> history = redemptionHistoryRepository
                .findByUserVoucherUserIdAndTenantIdOrderByRedeemedAtDesc(
                        userId,
                        TenantContext.requireTenantId());
        log.info("action=getUserRedemptionHistory completed | userId={} resultCount={}", userId, history.size());
        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucher> getAdminFilteredVouchers(
            Long assignedUserId,
            BigDecimal minVoucherAmount,
            BigDecimal maxVoucherAmount,
            String redemptionState,
            LocalDate issuedFrom,
            LocalDate issuedTo,
            LocalDate expiryFrom,
            LocalDate expiryTo,
            String status) {
        log.debug("action=getAdminFilteredVouchers | assignedUserId={} redemptionState={} status={}", assignedUserId,
                redemptionState, status);
        if (minVoucherAmount != null && maxVoucherAmount != null && minVoucherAmount.compareTo(maxVoucherAmount) > 0) {
            throw new IllegalArgumentException("minVoucherAmount cannot be greater than maxVoucherAmount");
        }
        if (issuedFrom != null && issuedTo != null && issuedFrom.isAfter(issuedTo)) {
            throw new IllegalArgumentException("issuedFrom cannot be after issuedTo");
        }
        if (expiryFrom != null && expiryTo != null && expiryFrom.isAfter(expiryTo)) {
            throw new IllegalArgumentException("expiryFrom cannot be after expiryTo");
        }

        String normalizedRedemptionState = normalizeRedemptionState(redemptionState);
        UserVoucherStatus normalizedStatus = parseStatus(status);

        LocalDateTime issuedFromDateTime = issuedFrom == null ? null : issuedFrom.atStartOfDay();
        LocalDateTime issuedToDateTime = issuedTo == null ? null : issuedTo.atTime(23, 59, 59);

        List<UserVoucher> results = userVoucherRepository.findAllByTenantIdWithAdminFilters(
                TenantContext.requireTenantId(),
                assignedUserId,
                minVoucherAmount,
                maxVoucherAmount,
                normalizedRedemptionState,
                issuedFromDateTime,
                issuedToDateTime,
                expiryFrom,
                expiryTo,
                normalizedStatus);
        log.info("action=getAdminFilteredVouchers completed | resultCount={}", results.size());
        return results;
    }

    private void validateTemplateEligibility(VoucherTemplate template) {
        if (!template.isEnabled()) {
            log.warn("action=validateTemplateEligibility failed | templateCode={} reason=Template disabled",
                    template.getCode());
            throw new IllegalArgumentException("Voucher template is disabled");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(template.getStartDate()) || today.isAfter(template.getExpiryDate())) {
            log.warn(
                    "action=validateTemplateEligibility failed | templateCode={} reason=Date outside validity window startDate={} expiryDate={} today={}",
                    template.getCode(), template.getStartDate(), template.getExpiryDate(), today);
            throw new IllegalArgumentException("Voucher template is not valid on this date");
        }
    }

    private UserVoucherStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserVoucherStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status. Allowed values: ACTIVE, INACTIVE");
        }
    }

    private String normalizeRedemptionState(String redemptionState) {
        if (redemptionState == null || redemptionState.isBlank()) {
            return null;
        }
        String normalized = redemptionState.trim().toUpperCase(Locale.ROOT);
        if (!"NOT_REDEEMED".equals(normalized)
                && !"PARTIALLY_REDEEMED".equals(normalized)
                && !"FULLY_REDEEMED".equals(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid redemptionState. Allowed values: NOT_REDEEMED, PARTIALLY_REDEEMED, FULLY_REDEEMED");
        }
        return normalized;
    }
}
