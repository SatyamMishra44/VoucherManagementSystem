package com.example.Voucher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VoucherManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoucherManagementSystemApplication.class, args);
    }

}
