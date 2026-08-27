package com.lecture.course.dto;

import com.lecture.course.entity.Course;
import com.lecture.course.entity.MaterialComponent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CourseDto {

    // 성분 한 건 (요청·응답 공용)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialComponentDto {

        // enum 으로 강제한다. 자유 문자열이면 추천 성분 교집합이 잡히지 않는다.
        @NotNull(message = "성분명은 필수입니다")
        private MaterialComponent.ComponentName name;

        @NotNull(message = "함량은 필수입니다")
        @DecimalMin(value = "0", message = "함량은 0 이상이어야 합니다")
        @DecimalMax(value = "100", message = "함량은 100 이하여야 합니다")
        private BigDecimal percentage;

        public static MaterialComponentDto from(MaterialComponent component) {
            return MaterialComponentDto.builder()
                    .name(component.getName())
                    .percentage(component.getPercentage())
                    .build();
        }

        public MaterialComponent toEntity() {
            return MaterialComponent.builder()
                    .name(this.name)
                    .percentage(this.percentage)
                    .build();
        }

        public static List<MaterialComponentDto> fromList(List<MaterialComponent> components) {
            if (components == null) return List.of();
            return components.stream()
                    .map(MaterialComponentDto::from)
                    .collect(Collectors.toList());
        }

        public static List<MaterialComponent> toEntityList(List<MaterialComponentDto> dtos) {
            if (dtos == null) return null;
            return dtos.stream()
                    .map(MaterialComponentDto::toEntity)
                    .collect(Collectors.toList());
        }
    }

    // 판매 로트 등록 요청 (공급기업)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "판매 로트명은 필수입니다")
        private String title;

        private String description;

        @NotNull(message = "카테고리는 필수입니다")
        private Course.Category category;

        @NotNull(message = "로트 총가격은 필수입니다")
        @PositiveOrZero(message = "로트 총가격은 0 이상이어야 합니다")
        private BigDecimal price;

        @PositiveOrZero(message = "수량은 0 이상이어야 합니다")
        private Integer quantity;

        private String region;

        @Valid
        private List<MaterialComponentDto> components;
    }

    // 판매 로트 수정 요청 (공급기업 본인 로트, 전체 필드)
    // null 로 들어온 필드는 변경하지 않는다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {

        private String title;
        private String description;
        private Course.Category category;

        @PositiveOrZero(message = "로트 총가격은 0 이상이어야 합니다")
        private BigDecimal price;

        @PositiveOrZero(message = "수량은 0 이상이어야 합니다")
        private Integer quantity;

        private String region;

        @Valid
        private List<MaterialComponentDto> components;
    }

    // 설명 보정 요청 (중간기업)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DescriptionUpdateRequest {

        @NotBlank(message = "원료 설명은 필수입니다")
        private String description;
    }

    // 승인·거절 요청 (중간기업)
    // reviewerId 는 본문에서 받지 않고 X-User-Id 로 기록한다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovalRequest {

        @NotNull(message = "승인 여부(decision)는 필수입니다")
        private Decision decision;

        private String rejectionReason;

        public enum Decision {
            APPROVED, REJECTED
        }
    }

    // 판매 로트 응답 (외부) - { success, message, data } 래퍼의 data 에 들어간다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MaterialLotResponse {
        private Long id;
        private String title;
        private String description;
        private Course.Category category;
        private BigDecimal price;
        private Integer quantity;
        private String region;
        private List<MaterialComponentDto> components;
        private Long supplierId;
        private String supplierName;
        private Integer contractCount;
        private Course.Status status;
        private Long reviewerId;
        private LocalDateTime reviewedAt;
        private String rejectionReason;
        private LocalDateTime createdAt;

        public static MaterialLotResponse from(Course course) {
            return MaterialLotResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .category(course.getCategory())
                    .price(course.getPrice())
                    .quantity(course.getQuantity())
                    .region(course.getRegion())
                    .components(MaterialComponentDto.fromList(course.getComponents()))
                    .supplierId(course.getInstructorId())
                    .supplierName(course.getSupplierName())
                    .contractCount(course.getEnrollmentCount())
                    .status(course.getStatus())
                    .reviewerId(course.getReviewerId())
                    .reviewedAt(course.getReviewedAt())
                    .rejectionReason(course.getRejectionReason())
                    .createdAt(course.getCreatedAt())
                    .build();
        }
    }

    // 판매 로트 응답 (내부 /internal/**) - 래퍼 없이 바로 반환
    // 외부 응답과 필드명만 다르다: supplierId -> instructorId, contractCount -> enrollmentCount
    // supplierName 은 내부 응답에 싣지 않는다
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseInternalResponse {
        private Long id;
        private String title;
        private String description;
        private Course.Category category;
        private BigDecimal price;
        private Integer quantity;
        private String region;
        private List<MaterialComponentDto> components;
        private Long instructorId;
        private Integer enrollmentCount;
        private Course.Status status;
        private Long reviewerId;
        private LocalDateTime reviewedAt;
        private String rejectionReason;
        private LocalDateTime createdAt;

        public static CourseInternalResponse from(Course course) {
            return CourseInternalResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .category(course.getCategory())
                    .price(course.getPrice())
                    .quantity(course.getQuantity())
                    .region(course.getRegion())
                    .components(MaterialComponentDto.fromList(course.getComponents()))
                    .instructorId(course.getInstructorId())
                    .enrollmentCount(course.getEnrollmentCount())
                    .status(course.getStatus())
                    .reviewerId(course.getReviewerId())
                    .reviewedAt(course.getReviewedAt())
                    .rejectionReason(course.getRejectionReason())
                    .createdAt(course.getCreatedAt())
                    .build();
        }
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

    // 추천 서비스용 응답 (카테고리 기반 구매 가능 로트 목록)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendResponse {
        private List<CourseInternalResponse> courses;
        private Course.Category category;
    }
}
