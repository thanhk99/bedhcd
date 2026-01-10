package com.api.bedhcd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrGenerateResponse {
    private String token;
    private String qrContent; // URL for the QR code
}
