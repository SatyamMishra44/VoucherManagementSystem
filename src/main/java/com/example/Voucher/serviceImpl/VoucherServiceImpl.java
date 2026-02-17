package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Voucher;
import com.example.Voucher.repository.VoucherRepository;
import com.example.Voucher.service.VoucherService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    // use constructor injection to inject the object
    public VoucherServiceImpl(
            VoucherRepository voucherRepository
    ) {
        this.voucherRepository = voucherRepository;
    }


    //created a new voucher and saved in repository means DB
    @Override
    public Voucher createVoucher(Voucher voucher) {
        if (voucher == null) {
            throw new IllegalArgumentException("Voucher cannot be null");
        }
        if (voucher.getAssignedUser() == null) {
            throw new IllegalArgumentException("Voucher must be assigned to a user");
        }
        if (voucher.getStartDate().isAfter(voucher.getExpiryDate())) {
            throw new IllegalArgumentException("Start date cannot be after expiry date");
        }
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
    public Optional<Voucher> getVoucherByIdForUser(Long voucherId, Long userId) {
        return voucherRepository.findByIdAndAssignedUserId(voucherId, userId);
    }

    @Override
    public Optional<Voucher> getVoucherByCodeForUser(String code, Long userId) {
        return voucherRepository.findByCodeAndAssignedUserId(code, userId);
    }

    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll(); // return list of all the voucher
    }

    @Override
    public List<Voucher> getAllVouchersForUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return voucherRepository.findByAssignedUserId(userId);
    }

    @Override
    public List<Voucher> getEligibleVouchers(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        LocalDate today = LocalDate.now();
        return voucherRepository.findEligibleVouchersForUser(userId, today);
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
        if(voucher.isRedeemed()){
            throw new RuntimeException("Voucher already redeemed");
        }
        return voucher;
    }
}
