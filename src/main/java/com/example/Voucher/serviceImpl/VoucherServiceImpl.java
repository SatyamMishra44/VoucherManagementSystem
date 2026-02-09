package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.Voucher;
import com.example.Voucher.repository.VoucherRepository;
import com.example.Voucher.service.VoucherService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    // use constructor injection to inject the object
    public VoucherServiceImpl(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
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
    public List<Voucher> getEligibleVouchers() {
        return voucherRepository.findAll()
                .stream()
                .filter(Voucher::isEnabled)
                .filter(voucher -> !voucher.hasExceededUsageLimit())
                .toList();
    }

    @Override
    public Voucher validateVoucher(String code) {
        Voucher voucher  = voucherRepository.findByCode(code)
                .orElseThrow(()-> new RuntimeException("Invalid voucher code"));

        if(!voucher.isEnabled()){
            throw new RuntimeException("voucher is disabled");
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
