package com.example.Voucher.controller;

import com.example.Voucher.dto.BillCreateRequestDto;
import com.example.Voucher.dto.BillResponseDto;
import com.example.Voucher.entity.Bill;
import com.example.Voucher.entity.User;
import com.example.Voucher.service.BillService;
import com.example.Voucher.service.CurrentUserService;
import com.example.Voucher.service.UserService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "4. Bills", description = "Billing APIs")
@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillService billService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    public BillController(
            BillService billService,
            UserService userService,
            CurrentUserService currentUserService
    ) {
        this.billService = billService;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority(@roleProperties.getAdmin())")
    public ResponseEntity<BillResponseDto> createBill(
            @Valid @RequestBody BillCreateRequestDto requestDto) {

        User user = userService.findById(requestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Bill bill = new Bill(user, requestDto.getTotalAmount());
        Bill savedBill = billService.createBill(bill);

        return new ResponseEntity<>(toResponse(savedBill), HttpStatus.CREATED);
    }

    @GetMapping("/{billId}")
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    public ResponseEntity<BillResponseDto> getBillById(@PathVariable Long billId) {
        boolean isAdmin = currentUserService.isCurrentUserAdmin();
        Optional<Bill> billOpt = isAdmin
                ? billService.getBillById(billId)
                : billService.getBillByIdForUser(billId, currentUserService.getCurrentUserId());
        return billOpt
                .map(bill -> ResponseEntity.ok(toResponse(bill)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority(@roleProperties.getAdmin(), @roleProperties.getUser())")
    public ResponseEntity<List<BillResponseDto>> getBillsByUserId(@PathVariable Long userId) {
        currentUserService.assertSelfOrAdmin(userId);
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
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setCreatedAt(bill.getCreatedAt());
        return dto;
    }
}
