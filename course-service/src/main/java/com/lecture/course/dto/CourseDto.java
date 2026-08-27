package com.lecture.course.dto;

import com.lecture.course.entity.Course;
import com.lecture.course.entity.MaterialComponent;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = """
                성분명. **enum으로 강제**한다. 자유 문자열을 허용하면 "리튬"/"LITHIUM"/"Li"가 섞여
                추천의 성분 교집합이 잡히지 않고 추천 결과가 항상 빈 배열이 된다.
                """, example = "LITHIUM")
        @NotNull(message = "성분명은 필수입니다")
        private MaterialComponent.ComponentName name;

        @Schema(description = "함량 (0~100)", example = "18.5")
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

        @Schema(description = "판매 로트명", example = "폐배터리 블랙매스 5톤")
        @NotBlank(message = "판매 로트명은 필수입니다")
        private String title;

        @Schema(description = "원료 설명", example = "전기차 배터리 파쇄 후 발생한 블랙매스")
        private String description;

        @NotNull(message = "카테고리는 필수입니다")
        private Course.Category category;

        @Schema(description = "**로트 전체의 총가격.** 단가가 아니다", example = "12000000")
        @NotNull(message = "로트 총가격은 필수입니다")
        @PositiveOrZero(message = "로트 총가격은 0 이상이어야 합니다")
        private BigDecimal price;

        @Schema(description = "로트 수량. 단위는 별도 필드로 두지 않으므로 필요하면 로트명이나 설명에 표기한다",
                example = "5")
        @PositiveOrZero(message = "수량은 0 이상이어야 합니다")
        private Integer quantity;

        @Schema(description = "공급 지역", example = "충북 청주")
        private String region;

        @Schema(description = "성분·함량 목록. 추천의 성분 매칭에 쓰인다")
        @Valid
        private List<MaterialComponentDto> components;
    }

    // 판매 로트 수정 요청 (공급기업 본인 로트, 전체 필드)
    // null 로 들어온 필드는 변경하지 않는다
    @Schema(description = "null로 보낸 필드는 변경하지 않는다. REJECTED 로트를 수정하면 PENDING으로 리셋된다")
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

        @Schema(description = "승인 또는 거절", example = "APPROVED")
        @NotNull(message = "승인 여부(decision)는 필수입니다")
        private Decision decision;

        @Schema(description = "거절 사유. decision이 REJECTED일 때 기록된다",
                example = "성분 함량 정보가 부족합니다.")
        private String rejectionReason;

        public enum Decision {
            APPROVED, REJECTED
        }
    }

    // 판매 로트 응답 (외부) - { success, message, data } 래퍼의 data 에 들어간다
    @Schema(name = "MaterialLotResponse",
            description = "외부 응답용 판매 로트. `{ success, message, data }` 래퍼의 data에 들어간다")
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
        @Schema(description = "성분·함량 목록")
        private List<MaterialComponentDto> components;

        @Schema(description = "공급기업 ID (내부 필드명 instructorId)", example = "2")
        private Long supplierId;

        @Schema(description = "공급기업명. 목록 조회 N+1을 피해 등록 시점에 비정규화 저장한 값",
                example = "SK순환자원")
        private String supplierName;

        @Schema(description = "계약 완료 건수 (내부 필드명 enrollmentCount). 로트 MVP에서는 0 또는 1",
                example = "0")
        private Integer contractCount;

        @Schema(description = """
                로트 상태. 신규 등록은 항상 PENDING이며, 목록·추천에는 APPROVED만 노출된다.
                RESERVED는 MVP에서 사용하지 않는다.
                """, example = "APPROVED")
        private Course.Status status;

        @Schema(description = "검토한 중간기업 ID. 승인·거절 시 X-User-Id로 기록된다", example = "7")
        private Long reviewerId;

        @Schema(description = "검토 일시. 서버가 생성한다")
        private LocalDateTime reviewedAt;

        @Schema(description = "거절 사유. 공급기업이 로트를 수정하면 초기화된다")
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
    @Schema(name = "CourseInternalResponse", description = """
            서비스 간 호출용 판매 로트. 래퍼 없이 바로 반환한다.
            MaterialLotResponse와 필드명만 다르며 supplierName은 싣지 않는다.
            """)
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
        @Schema(description = "성분·함량 목록. 추천 서비스의 성분 매칭에 쓰인다")
        private List<MaterialComponentDto> components;

        @Schema(description = "공급기업 ID (외부 응답의 supplierId)", example = "2")
        private Long instructorId;

        @Schema(description = "계약 완료 건수 (외부 응답의 contractCount)", example = "0")
        private Integer enrollmentCount;

        @Schema(description = "로트 상태. 내부 상세 조회는 상태 필터가 없어 SOLD도 반환된다",
                example = "APPROVED")
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
