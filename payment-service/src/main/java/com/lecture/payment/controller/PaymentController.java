package com.lecture.payment.controller;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "계약금 결제", description = """
        판매 로트 계약금 결제. 실 PG 연동이 아니라 시뮬레이션이며 항상 성공으로 처리한다.

        금액은 Enrollment Service가 Course Service에서 조회한 **로트 총가격**을 그대로 전달한다.
        결제가 완료되면 `payment.completed` 이벤트를 Kafka로 발행해 계약을 활성화한다.

        외부 응답은 `buyerId`(구매기업), `materialLotId`(판매 로트) 용어를 쓰고,
        내부 요청은 기존 계약대로 `userId`, `courseId`를 유지한다.
        """)
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payments/internal/request - 내부 결제 요청 (Enrollment Service 호출)
     */
    @Operation(summary = "[내부] 계약금 결제 요청", description = """
            Enrollment Service가 계약 신청 시 호출한다. 래퍼 없이 `{ paymentId, status }`를 반환한다.

            `amount`는 등록된 로트 총가격이어야 한다. 하드코딩 금액을 보내지 않는다.
            결제가 완료되면 `payment.completed` 이벤트가 발행되고, Enrollment Service가 이를 받아
            계약을 `ACTIVE`로 바꾸고 로트를 `SOLD`로 전환한다.
            """)
    @PostMapping("/internal/request")
    public ResponseEntity<PaymentDto.InternalPaymentResult> processInternalPayment(
            @RequestBody PaymentDto.InternalPaymentRequest request) {

        PaymentDto.InternalPaymentResult result = paymentService.processInternalPayment(request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /payments/{id} - 결제 단건 조회
     */
    @Operation(summary = "계약금 결제 단건 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "없는 결제 ID")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.ApiResponse<PaymentDto.PaymentResponse>> getPayment(
            @Parameter(description = "결제 ID", example = "1") @PathVariable Long id) {

        return ResponseEntity.ok(
                PaymentDto.ApiResponse.success(paymentService.getPayment(id)));
    }

    /**
     * GET /payments/user/{userId} - 사용자 결제 내역 조회
     */
    @Operation(summary = "기업 계약금 결제 내역 조회",
            description = "해당 구매기업의 결제 내역을 반환한다. `amount`는 로트 총가격이다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<PaymentDto.ApiResponse<List<PaymentDto.PaymentResponse>>> getPaymentsByUser(
            @Parameter(description = "구매기업 ID (내부 필드명 userId)", example = "3")
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                PaymentDto.ApiResponse.success(paymentService.getPaymentsByUser(userId)));
    }
}
