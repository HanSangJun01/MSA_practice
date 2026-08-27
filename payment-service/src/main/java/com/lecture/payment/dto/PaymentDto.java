package com.lecture.payment.dto;

import com.lecture.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentDto {

    // 결제 요청 (외부 클라이언트용)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentRequest {
        @NotNull(message = "판매 로트 ID는 필수입니다")
        private Long courseId;

        @NotNull(message = "금액은 필수입니다")
        @Positive(message = "금액은 양수여야 합니다")
        private BigDecimal amount;
    }

    // 내부 서비스 결제 요청 (Enrollment Service → Payment Service)
    // 내부 필드명 userId·courseId 는 기존 계약을 유지하고
    // amount 는 Enrollment Service 가 조회한 로트 총가격을 그대로 받는다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternalPaymentRequest {

        @Schema(description = "구매기업 ID (내부 필드명 유지)", example = "3")
        private Long userId;

        @Schema(description = "판매 로트 ID (내부 필드명 유지)", example = "10")
        private Long courseId;

        @Schema(description = "로트 총가격. 하드코딩 금액을 보내지 않는다", example = "12000000")
        private BigDecimal amount;
    }

    // 계약금 결제 응답 (외부)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private Long paymentId;

        @Schema(description = "구매기업 ID (내부 필드명 userId)", example = "3")
        private Long buyerId;

        @Schema(description = "판매 로트 ID (내부 필드명 courseId)", example = "10")
        private Long materialLotId;

        @Schema(description = "로트 총가격", example = "12000000")
        private BigDecimal amount;
        private Payment.Status status;
        private String transactionId;
        private LocalDateTime createdAt;

        public static PaymentResponse from(Payment payment) {
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .buyerId(payment.getUserId())
                    .materialLotId(payment.getCourseId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .transactionId(payment.getTransactionId())
                    .createdAt(payment.getCreatedAt())
                    .build();
        }
    }

    // 내부 서비스 결제 결과 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternalPaymentResult {
        private Long paymentId;
        private String status;
    }

    // 공통 API 응답 래퍼
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
