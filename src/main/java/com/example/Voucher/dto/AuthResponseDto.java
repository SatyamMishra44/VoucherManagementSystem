package com.example.Voucher.dto;

import lombok.Data;

@Data
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long accessTokenExpiresInSeconds;
    private long refreshTokenExpiresInSeconds;

    public AuthResponseDto(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds,
            long refreshTokenExpiresInSeconds
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
        this.refreshTokenExpiresInSeconds = refreshTokenExpiresInSeconds;
    }
}
