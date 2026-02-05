package com.example.Voucher.controller;

import com.example.Voucher.dto.BillCreateRequestDto;
import com.example.Voucher.dto.BillResponseDto;
import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.service.BillService;
import com.example.Voucher.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillService billService;
    private final UserService userService;

    public BillController(BillService billService, UserService userService) {
        this.billService = billService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BillResponseDto> createBill(
            @Valid @RequestBody BillCreateRequestDto requestDto) {

        User user = userService.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bill bill = new Bill(user, requestDto.getOriginalAmount().intValue());
        Bill savedBill = billService.createBill(bill);

        return new ResponseEntity<>(toResponse(savedBill), HttpStatus.CREATED);
    }

    @GetMapping("/{billId}")
    public ResponseEntity<BillResponseDto> getBillById(@PathVariable Long billId) {
        Optional<Bill> billOpt = billService.getBillById(billId);
        return billOpt
                .map(bill -> ResponseEntity.ok(toResponse(bill)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BillResponseDto>> getBillsByUserId(@PathVariable Long userId) {
        List<BillResponseDto> bills = billService.getBillsByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(bills);
    }

    private BillResponseDto toResponse(Bill bill) {
        BillResponseDto dto = new BillResponseDto();
        dto.setBillId(bill.getId());
        dto.setUserId(bill.getUser().getId());
        dto.setOriginalAmount(bill.getTotalAmount().doubleValue());
        dto.setDiscountAmount(0.0);
        dto.setFinalPayableAmount(bill.getTotalAmount().doubleValue());
        dto.setBillStatus("CREATED");
        dto.setCreatedAt(bill.getCreatedAt());
        return dto;
    }
}
