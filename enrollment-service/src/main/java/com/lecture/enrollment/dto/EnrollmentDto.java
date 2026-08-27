package com.lecture.enrollment.dto;

import com.lecture.enrollment.entity.Enrollment;
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
    public static class MaterialLotSummary {
        private Long id;
        private String title;
        private String description;
        private String category;
        private BigDecimal price;
        private Integer quantity;
        private String region;
        private List<MaterialComponent> components;
        private String supplierName;
        private Integer contractCount;
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
        private Long buyerId;
        private Long materialLotId;
        private Enrollment.Status status;
        private LocalDateTime createdAt;

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
    public static class PurchasedLot {
        private Long courseId;
        private String category;
        private List<MaterialComponent> components;
    }

    // 추천 서비스용: 구매 이력 조회 응답 (래퍼 없음, 내부 필드명 userId·courseId 유지)
    // activeCourseIds 는 기존 필드라 그대로 둔다 (담당자의 pydantic 모델 호환)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentHistoryResponse {
        private Long userId;
        private List<Long> activeCourseIds;
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
