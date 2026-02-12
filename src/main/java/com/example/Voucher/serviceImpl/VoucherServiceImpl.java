package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Voucher;
import com.example.Voucher.repository.VoucherRedemptionRepository;
import com.example.Voucher.repository.VoucherRepository;
import com.example.Voucher.service.VoucherService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;

    // use constructor injection to inject the object
    public VoucherServiceImpl(
            VoucherRepository voucherRepository,
            VoucherRedemptionRepository voucherRedemptionRepository
    ) {
        this.voucherRepository = voucherRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
    }


    //created a new voucher and saved in repository means DB
    @Override
    public Voucher createVoucher(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher updateVoucherStatus(Long VoucherId, boolean isEnabled) {
        Voucher voucher = voucherRepository.findById(VoucherId)
                .orElseThrow(()->new RuntimeException("Voucher not found with id: " + VoucherId));

        // this is the domain-driven state change not a setter
        if(isEnabled){
            voucher.enable();
        }else{
            voucher.disable();
        }
        return voucherRepository.save(voucher);// save the changes in the repository
    }

    @Override
    public Optional<Voucher> getVoucherById(Long VoucherId) {
        return voucherRepository.findById(VoucherId);
    }

    @Override
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll(); // return list of all the voucher
    }

    @Override
    public List<Voucher> getEligibleVouchers(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        LocalDate today = LocalDate.now();
        return voucherRepository.findAll()
                .stream()
                .filter(Voucher::isEnabled)
                .filter(voucher -> isWithinDateRange(voucher, today))
                .filter(voucher -> !voucher.hasExceededUsageLimit())
                .filter(voucher -> !hasExceededUserLimit(voucher, userId))
                .toList();
    }

    private boolean isWithinDateRange(Voucher voucher, LocalDate today) {
        return (today.isEqual(voucher.getStartDate()) || today.isAfter(voucher.getStartDate()))
                && (today.isEqual(voucher.getExpiryDate()) || today.isBefore(voucher.getExpiryDate()));
    }

    private boolean hasExceededUserLimit(Voucher voucher, Long userId) {
        // Single-use per user: any previous redemption makes it ineligible.
        return voucherRedemptionRepository.existsByVoucherIdAndUserId(
                voucher.getId(),
                userId
        );
    }

    @Override
    public Voucher validateVoucher(String code) {
        Voucher voucher  = voucherRepository.findByCode(code)
                .orElseThrow(()-> new RuntimeException("Invalid voucher code"));

        if(!voucher.isEnabled()){
            throw new RuntimeException("voucher is disabled");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(voucher.getStartDate()) || today.isAfter(voucher.getExpiryDate())) {
            throw new RuntimeException("voucher is not valid on this date");
        }
        if(voucher.hasExceededUsageLimit()){
            throw new RuntimeException("Voucher usage limit exceeded can't be used now");
        }
        return voucher;
    }

    @Override
    public void incrementVoucherUsage(Long voucherId) {
        Voucher voucher  = voucherRepository.findById(voucherId)
                .orElseThrow(()-> new RuntimeException("Voucher not found"));

        voucher.incrementUsage();
        voucherRepository.save(voucher);
    }
}
