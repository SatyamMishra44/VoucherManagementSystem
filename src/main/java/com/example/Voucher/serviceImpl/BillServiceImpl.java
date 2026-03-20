package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Bill;
import com.example.Voucher.repository.BillRepository;
import com.example.Voucher.service.BillService;
import com.example.Voucher.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillServiceImpl implements BillService {
    private static final Logger log = LoggerFactory.getLogger(BillServiceImpl.class);

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

        if (bill.getTotalAmount() == null || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bill amount must be greater than zero");
        }

        log.info("action=createBill started | userId={} amount={}", bill.getUser().getId(), bill.getTotalAmount());
        Bill saved = billRepository.save(bill);
        log.info("action=createBill completed | billId={} userId={} amount={}", saved.getId(), saved.getUser().getId(),
                saved.getTotalAmount());
        return saved;
    }

    /**
     * Fetch bill by id
     */
    @Override
    public Optional<Bill> getBillById(Long billId) {

        if (billId == null) {
            throw new IllegalArgumentException("Bill ID cannot be null");
        }

        log.debug("action=getBillById | billId={}", billId);
        Optional<Bill> result = billRepository.findByIdAndTenantId(billId, TenantContext.requireTenantId());
        if (result.isEmpty()) {
            log.warn("action=getBillById | billId={} result=not found", billId);
        }
        return result;
    }

    @Override
    public Optional<Bill> getBillByIdForUser(Long billId, Long userId) {
        if (billId == null) {
            throw new IllegalArgumentException("Bill ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        log.debug("action=getBillByIdForUser | billId={} userId={}", billId, userId);
        Optional<Bill> result = billRepository.findByIdAndUserIdAndTenantId(billId, userId,
                TenantContext.requireTenantId());
        if (result.isEmpty()) {
            log.warn("action=getBillByIdForUser | billId={} userId={} result=not found", billId, userId);
        }
        return result;
    }

    /**
     * Fetch all bills of a user
     */
    @Override
    public List<Bill> getBillsByUserId(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.debug("action=getBillsByUserId | userId={}", userId);
        List<Bill> bills = billRepository.findByUserIdAndTenantId(userId, TenantContext.requireTenantId());
        log.info("action=getBillsByUserId completed | userId={} resultCount={}", userId, bills.size());
        return bills;
    }

    @Override
    public BigDecimal calculateTotalAmount(Long billId) {
        log.debug("action=calculateTotalAmount | billId={}", billId);
        return getBillById(billId)
                .map(Bill::getTotalAmount)
                .orElseThrow(() -> {
                    log.error("action=calculateTotalAmount failed | billId={} reason=Bill not found", billId);
                    return new RuntimeException("Bill not found");
                });
    }
}
