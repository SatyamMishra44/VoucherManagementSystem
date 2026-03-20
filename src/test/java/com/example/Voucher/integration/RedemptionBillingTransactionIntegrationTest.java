package com.example.Voucher.integration;

import com.example.Voucher.dto.AuthResponseDto;
import com.example.Voucher.dto.BillCreateRequestDto;
import com.example.Voucher.dto.BillResponseDto;
import com.example.Voucher.dto.RedemptionHistoryResponseDto;
import com.example.Voucher.dto.TransactionResponseDto;
import com.example.Voucher.dto.UserVoucherResponseDto;
import com.example.Voucher.dto.VoucherPurchaseRequestDto;
import com.example.Voucher.dto.VoucherRedeemRequestDto;
import com.example.Voucher.dto.VoucherRedeemResponseDto;
import com.example.Voucher.entity.Role;
import com.example.Voucher.entity.User;
import com.example.Voucher.entity.VoucherTemplate;
import com.example.Voucher.tenant.Tenant;
import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RedemptionBillingTransactionIntegrationTest extends IntegrationTestBase {

    @Test
    void user_canPurchaseRedeem_andTransactionsAndBillsRecorded() {
        Tenant systemTenant = systemTenant();
        Role platformAdminRole = roleByName(roleProperties.getPlatformAdmin());
        Role userRole = roleByName(roleProperties.getUser());

        String adminPassword = "AdminPass123!";
        User platformAdmin = createUser(
                "Platform",
                "Admin",
                uniqueEmail("platform-admin"),
                uniquePhone(),
                adminPassword,
                platformAdminRole,
                systemTenant
        );
        AuthResponseDto platformAuth = login(platformAdmin.getEmail(), adminPassword, null);

        String userPassword = "UserPass123!";
        User user = createUser(
                "System",
                "User",
                uniqueEmail("user"),
                uniquePhone(),
                userPassword,
                userRole,
                systemTenant
        );
        AuthResponseDto userAuth = login(user.getEmail(), userPassword, null);

        VoucherTemplate template = createVoucherTemplate("VCH-" + System.currentTimeMillis(), systemTenant);

        VoucherPurchaseRequestDto purchaseRequest = new VoucherPurchaseRequestDto();
        purchaseRequest.setVoucherCode(template.getCode());
        purchaseRequest.setQuantity(2);

        ResponseEntity<UserVoucherResponseDto> purchaseResponse = restTemplate.exchange(
                "/api/v1/vouchers/purchase",
                HttpMethod.POST,
                new HttpEntity<>(purchaseRequest, authHeaders(userAuth.getAccessToken(), null)),
                UserVoucherResponseDto.class
        );
        Assertions.assertThat(purchaseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserVoucherResponseDto purchasedVoucher = purchaseResponse.getBody();
        Assertions.assertThat(purchasedVoucher).isNotNull();
        Assertions.assertThat(purchasedVoucher.getRemainingBalance())
                .isEqualByComparingTo(purchasedVoucher.getTotalPurchasedAmount());

        BillCreateRequestDto billRequest = new BillCreateRequestDto();
        billRequest.setUserId(user.getId());
        billRequest.setTotalAmount(new BigDecimal("15.00"));

        ResponseEntity<BillResponseDto> billResponse = restTemplate.exchange(
                "/api/v1/bills",
                HttpMethod.POST,
                new HttpEntity<>(billRequest, authHeaders(platformAuth.getAccessToken(), null)),
                BillResponseDto.class
        );
        Assertions.assertThat(billResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BillResponseDto bill = billResponse.getBody();
        Assertions.assertThat(bill).isNotNull();

        VoucherRedeemRequestDto redeemRequest = new VoucherRedeemRequestDto();
        redeemRequest.setUserVoucherId(purchasedVoucher.getId());
        redeemRequest.setBillId(bill.getBillId());

        ResponseEntity<VoucherRedeemResponseDto> redeemResponse = restTemplate.exchange(
                "/api/v1/vouchers/redeem",
                HttpMethod.POST,
                new HttpEntity<>(redeemRequest, authHeaders(userAuth.getAccessToken(), null)),
                VoucherRedeemResponseDto.class
        );
        Assertions.assertThat(redeemResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        VoucherRedeemResponseDto redeemBody = redeemResponse.getBody();
        Assertions.assertThat(redeemBody).isNotNull();
        Assertions.assertThat(redeemBody.getBillId()).isEqualTo(bill.getBillId());
        Assertions.assertThat(redeemBody.getUserVoucherId()).isEqualTo(purchasedVoucher.getId());
        Assertions.assertThat(redeemBody.getBillAmount().subtract(redeemBody.getRedeemedAmount()))
                .isEqualByComparingTo(redeemBody.getPayableAmount());

        ResponseEntity<List<RedemptionHistoryResponseDto>> redemptionResponse = restTemplate.exchange(
                "/api/v1/vouchers/redemptions",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<RedemptionHistoryResponseDto>>() {}
        );
        Assertions.assertThat(redemptionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(redemptionResponse.getBody())
                .extracting(RedemptionHistoryResponseDto::getRedemptionId)
                .contains(redeemBody.getRedemptionId());

        ResponseEntity<BillResponseDto> billFetchResponse = restTemplate.exchange(
                "/api/v1/bills/" + bill.getBillId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAuth.getAccessToken(), null)),
                BillResponseDto.class
        );
        Assertions.assertThat(billFetchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(billFetchResponse.getBody()).isNotNull();

        ResponseEntity<List<BillResponseDto>> billsByUserResponse = restTemplate.exchange(
                "/api/v1/bills/user/" + user.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<BillResponseDto>>() {}
        );
        Assertions.assertThat(billsByUserResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(billsByUserResponse.getBody())
                .extracting(BillResponseDto::getBillId)
                .contains(bill.getBillId());

        ResponseEntity<List<TransactionResponseDto>> userTxResponse = restTemplate.exchange(
                "/api/v1/transactions/user/" + user.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<TransactionResponseDto>>() {}
        );
        Assertions.assertThat(userTxResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(userTxResponse.getBody())
                .extracting(TransactionResponseDto::getBillId)
                .contains(bill.getBillId());

        ResponseEntity<List<TransactionResponseDto>> adminTxResponse = restTemplate.exchange(
                "/api/v1/transactions?userId=" + user.getId(),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(platformAuth.getAccessToken(), null)),
                new ParameterizedTypeReference<List<TransactionResponseDto>>() {}
        );
        Assertions.assertThat(adminTxResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(adminTxResponse.getBody())
                .extracting(TransactionResponseDto::getBillId)
                .contains(bill.getBillId());
    }
}
