package com.lecture.enrollment.dto;

import com.lecture.enrollment.entity.Enrollment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EnrollmentDto {

    // 구매·계약 신청 요청
    // 내부 필드명 courseId 는 기존 계약을 유지하고 의미만 판매 로트 ID 로 해석한다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollRequest {
        @Schema(description = "판매 로트 ID. 내부 필드명 courseId를 기존 계약대로 유지한다", example = "10")
        @NotNull(message = "판매 로트 ID는 필수입니다")
        private Long courseId;
    }

    // 성분 한 건 (추천 서비스용 구매 이력에 실린다)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialComponent {
        private String name;
        private BigDecimal percentage;
    }

    // 판매 로트 요약 정보 (내 계약 목록 표시용)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "계약 목록에 붙는 판매 로트 요약. 내부 상세 조회로 조립하므로 SOLD 로트도 실린다")
    public static class MaterialLotSummary {
        private Long id;
        private String title;
        private String description;
        private String category;
        private BigDecimal price;
        private Integer quantity;
        private String region;
        private List<MaterialComponent> components;

        @Schema(description = "공급기업명. 내부 로트 응답에 없어 instructorId로 user-service에서 조달한다",
                example = "SK순환자원")
        private String supplierName;

        @Schema(description = "계약 완료 건수", example = "1")
        private Integer contractCount;

        @Schema(description = "로트 상태. 계약 완료 후에는 SOLD다", example = "SOLD")
        private String status;
        private String thumbnail;
    }

    // 계약 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentResponse {
        private Long id;

        @Schema(description = "구매기업 ID (내부 필드명 userId)", example = "3")
        private Long buyerId;

        @Schema(description = "판매 로트 ID (내부 필드명 courseId)", example = "10")
        private Long materialLotId;

        @Schema(description = """
                계약 상태. 신청 직후에는 PENDING이고, 결제 완료 이벤트를 받으면 ACTIVE가 된다.
                결제에 실패하면 CANCELLED로 정리되어 같은 로트를 다시 신청할 수 있다.
                """, example = "ACTIVE")
        private Enrollment.Status status;

        private LocalDateTime createdAt;

        @Schema(description = "판매 로트 요약. 목록 조회에서만 채워진다")
        private MaterialLotSummary material;

        public static EnrollmentResponse from(Enrollment enrollment) {
            return EnrollmentResponse.builder()
                    .id(enrollment.getId())
                    .buyerId(enrollment.getUserId())
                    .materialLotId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .createdAt(enrollment.getCreatedAt())
                    .build();
        }

        public static EnrollmentResponse from(Enrollment enrollment, MaterialLotSummary material) {
            return EnrollmentResponse.builder()
                    .id(enrollment.getId())
                    .buyerId(enrollment.getUserId())
                    .materialLotId(enrollment.getCourseId())
                    .status(enrollment.getStatus())
                    .createdAt(enrollment.getCreatedAt())
                    .material(material)
                    .build();
        }
    }

    // 추천 서비스용: 구매한 로트 한 건의 카테고리·성분
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "구매한 로트 한 건의 카테고리·성분. 추천 서비스가 성분 교집합을 계산하는 입력이다")
    public static class PurchasedLot {

        @Schema(description = "판매 로트 ID", example = "10")
        private Long courseId;

        @Schema(description = "산업 부산물 카테고리", example = "BATTERY")
        private String category;

        private List<MaterialComponent> components;
    }

    // 추천 서비스용: 구매 이력 조회 응답 (래퍼 없음, 내부 필드명 userId·courseId 유지)
    // activeCourseIds 는 기존 필드라 그대로 둔다 (담당자의 pydantic 모델 호환)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "추천 서비스용 구매 이력. 래퍼 없이 반환하며 내부 필드명 userId·courseId를 유지한다")
    public static class EnrollmentHistoryResponse {

        @Schema(description = "구매기업 ID", example = "3")
        private Long userId;

        @Schema(description = "ACTIVE 계약의 로트 ID 목록. 기존 필드라 그대로 둔다")
        private List<Long> activeCourseIds;

        @Schema(description = "구매한 로트별 카테고리와 성분. 호출 한 번으로 추천 입력이 완성되도록 함께 싣는다")
        private List<PurchasedLot> purchasedLots;
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
