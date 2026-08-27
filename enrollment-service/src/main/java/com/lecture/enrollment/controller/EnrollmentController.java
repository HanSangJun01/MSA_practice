package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "구매·계약", description = """
        구매기업의 판매 로트 구매·계약 신청.

        MVP에서는 한 로트를 한 구매기업이 전체 구매한다. 부분 구매와 재고 분할은 범위 밖이다.
        승인된(`APPROVED`) 로트만 계약할 수 있고, 계약이 완료되면 로트는 `SOLD`가 되어
        더 이상 구매 목록이나 추천 후보에 나오지 않는다.

        경로와 내부 필드명(`userId`, `courseId`)은 Gateway·Kafka 호환을 위해 유지하고,
        외부 응답에서만 `buyerId`, `materialLotId`, `material` 용어로 변환한다.
        """)
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * POST /enrollments - 수강신청
     * Gateway에서 X-User-Id 헤더로 사용자 ID 전달
     */
    @Operation(summary = "판매 로트 구매·계약 신청", description = """
            구매기업이 판매 로트 계약을 신청한다. 처리 순서는 다음과 같다.

            1. 구매 가능한(`APPROVED`) 로트인지 확인
            2. 중복 계약 확인
            3. 계약을 `PENDING`으로 생성
            4. Course Service에서 조회한 **로트 총가격**으로 계약금 결제 요청

            결제가 완료되면 Kafka 이벤트를 받아 계약이 `ACTIVE`가 되고 로트가 `SOLD`로 전환된다.
            응답 시점에는 아직 `PENDING`일 수 있다.

            결제에 실패하면 생성한 계약을 `CANCELLED`로 정리해 같은 로트를 다시 신청할 수 있다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "계약 신청 완료 (결제 대기)"),
            @ApiResponse(responseCode = "400",
                    description = "승인되지 않았거나 이미 판매된 로트, 중복 계약, 또는 `X-User-Id` 헤더 누락"),
            @ApiResponse(responseCode = "503", description = "결제 실패 또는 연계 서비스 연결 실패")
    })
    @PostMapping
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> enroll(
            @Valid @RequestBody EnrollmentDto.EnrollRequest request,
            @Parameter(description = "Gateway가 주입하는 구매기업 ID", required = true, example = "3")
            @RequestHeader("X-User-Id") Long userId) {

        EnrollmentDto.EnrollmentResponse response =
                enrollmentService.enroll(userId, request.getCourseId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/my - 내 수강 목록 조회
     * Gateway가 전달한 X-User-Id 헤더를 사용
     */
    @Operation(summary = "내 계약 목록 조회", description = """
            토큰 주체의 계약 목록을 반환한다. 각 건에 판매 로트 요약(`material`)이 붙는다.

            로트 정보는 상태 필터가 없는 내부 상세 조회로 조립하므로 `SOLD` 로트도 정상적으로 실린다.
            공급기업명은 내부 응답에 없어 `instructorId`로 user-service에서 따로 조달한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "`X-User-Id` 헤더 누락")
    })
    @GetMapping("/my")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.EnrollmentResponse>>> getMyEnrollments(
            @Parameter(description = "Gateway가 주입하는 구매기업 ID", required = true, example = "3")
            @RequestHeader("X-User-Id") Long userId) {

        List<EnrollmentDto.EnrollmentResponse> response =
                enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/user/{userId} - 특정 사용자 수강 목록 조회
     */
    @Operation(summary = "특정 기업 계약 목록 조회",
            description = "지정한 구매기업의 계약 목록을 반환한다. 응답 형태는 `/my`와 같다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.EnrollmentResponse>>> getEnrollments(
            @Parameter(description = "구매기업 ID (내부 필드명 userId)", example = "3")
            @PathVariable Long userId) {

        List<EnrollmentDto.EnrollmentResponse> response =
                enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/internal/history/{userId} - 수강 이력 조회 (Recommend Service용)
     */
    @Operation(summary = "[내부] 구매 이력 및 성분 조회 (추천 서비스용)", description = """
            Recommend Service가 성분 기반 추천을 만들 때 쓰는 입력이다. 래퍼 없이 반환한다.

            `ACTIVE` 계약만 대상으로 하며, 구매한 로트마다 `category`와 `components`를 함께 실어
            호출 한 번으로 추천 입력이 완성되게 한다.

            구매 완료 로트는 `SOLD`라 `GET /api/courses`에는 나오지 않으므로,
            조립은 상태 필터가 없는 `GET /api/courses/internal/{id}`로 한다.
            `activeCourseIds`는 기존 필드라 그대로 유지한다.
            """)
    @GetMapping("/internal/history/{userId}")
    public ResponseEntity<EnrollmentDto.EnrollmentHistoryResponse> getEnrollmentHistory(
            @Parameter(description = "구매기업 ID", example = "3") @PathVariable Long userId) {

        return ResponseEntity.ok(enrollmentService.getEnrollmentHistory(userId));
    }
}