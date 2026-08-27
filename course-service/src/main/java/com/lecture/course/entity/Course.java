package com.lecture.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 순환원료 판매 로트
 *
 * Course 한 건은 재사용 가능한 상품 마스터가 아니라 판매 가능한 원료 로트 한 건이다.
 * MVP 에서는 한 로트를 한 구매기업이 전체 구매한다 (부분 구매·재고 분할 없음).
 *
 * 테이블명과 내부 필드명(instructorId, enrollmentCount)은 Gateway·Kafka 호환을 위해 유지하고
 * 비즈니스 의미만 공급기업 ID·계약 완료 건수로 해석한다.
 */
@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 판매 로트명 */
    @Column(nullable = false)
    private String title;

    /** 원료 설명 */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    /** 로트 총가격 (B2B 금액이므로 DECIMAL(18,2)) */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    /** 로트 수량 (unit 은 MVP 범위 밖 - 로트명/설명에 자연어로 표기) */
    private Integer quantity;

    /** 공급 지역 (일정상 enum 대신 문자열) */
    @Column(length = 100)
    private String region;

    /** 성분·함량 목록 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_components", joinColumns = @JoinColumn(name = "course_id"))
    @Builder.Default
    private List<MaterialComponent> components = new ArrayList<>();

    /** 공급기업 ID (users 테이블 참조 - 직접 JOIN 없이 ID만 보관) */
    @Column(nullable = false)
    private Long instructorId;

    /** 공급기업명 - 목록 조회 N+1 방지를 위해 등록 시점에 비정규화 저장 */
    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    /** 계약 완료 건수 (로트 MVP 에서는 0 또는 1) */
    @Column(nullable = false)
    @Builder.Default
    private Integer enrollmentCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    /** 검토 중간기업 ID */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /** 검토 일시 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 거절 사유 */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 산업 부산물 카테고리 */
    public enum Category {
        METAL, PLASTIC, BATTERY, ELECTRONIC, CHEMICAL, CONSTRUCTION, TEXTILE, OTHER
    }

    /**
     * 로트 상태
     *
     * RESERVED 는 MVP 범위에서 사용하지 않는다.
     * 결제가 동기 시뮬레이션이라 선점 구간이 사실상 없고,
     * 중복 계약은 결제 완료 직후 SOLD 전환 + isPurchasable() 검사로 막는다.
     */
    public enum Status {
        PENDING, APPROVED, REJECTED, RESERVED, SOLD, WITHDRAWN
    }

    // ----- 상태 전이 -----
    // 아래 표에 없는 전이는 IllegalStateException 으로 거절한다 (400)
    //   (신규)   -> PENDING    공급기업 로트 등록
    //   PENDING  -> APPROVED   중간기업 승인
    //   PENDING  -> REJECTED   중간기업 거절
    //   REJECTED -> PENDING    공급기업 로트 수정 시 자동 리셋
    //   APPROVED -> SOLD       계약 결제 완료 이벤트
    //   APPROVED -> WITHDRAWN  공급기업 판매 철회 (미계약 상태에서만)
    //   PENDING  -> WITHDRAWN  공급기업 판매 철회

    /** 중간기업 승인 */
    public void approve(Long reviewerId) {
        requireStatus(Status.PENDING, "승인");
        this.status = Status.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    /** 중간기업 거절 */
    public void reject(Long reviewerId, String reason) {
        requireStatus(Status.PENDING, "거절");
        this.status = Status.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * 공급기업 본인 로트 수정 (PENDING 또는 REJECTED 에서만)
     * REJECTED 로트를 수정하면 PENDING 으로 되돌리고 검토 기록을 초기화한다.
     * null 로 들어온 필드는 변경하지 않는다.
     */
    public void update(String title, String description, Category category, BigDecimal price,
                       Integer quantity, String region, List<MaterialComponent> components) {

        if (this.status != Status.PENDING && this.status != Status.REJECTED) {
            throw new IllegalStateException(
                    "PENDING 또는 REJECTED 로트만 수정할 수 있습니다. 현재 상태: " + this.status);
        }

        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (price != null) this.price = price;
        if (quantity != null) this.quantity = quantity;
        if (region != null) this.region = region;
        if (components != null) {
            if (this.components == null) {
                this.components = new ArrayList<>();
            }
            this.components.clear();
            this.components.addAll(components);
        }

        if (this.status == Status.REJECTED) {
            this.status = Status.PENDING;
            this.reviewerId = null;
            this.reviewedAt = null;
            this.rejectionReason = null;
        }
    }

    /**
     * 중간기업 설명 보정 - 검토 행위의 일부이지 승인 절차가 아니다.
     * status, reviewerId, reviewedAt, rejectionReason 을 건드리지 않는다.
     */
    public void updateDescription(String description) {
        if (this.status != Status.PENDING && this.status != Status.APPROVED) {
            throw new IllegalStateException(
                    "PENDING 또는 APPROVED 로트만 설명을 보정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.description = description;
    }

    /** 공급기업 판매 철회 (미계약 상태에서만) */
    public void withdraw() {
        if (this.status != Status.PENDING && this.status != Status.APPROVED) {
            throw new IllegalStateException(
                    "PENDING 또는 APPROVED 로트만 철회할 수 있습니다. 현재 상태: " + this.status);
        }
        if (this.enrollmentCount > 0) {
            throw new IllegalStateException("이미 계약된 로트는 철회할 수 없습니다");
        }
        this.status = Status.WITHDRAWN;
    }

    /** MVP 미사용 - 결제가 동기 시뮬레이션이라 선점 구간이 없다 */
    public void reserve() {
        requireStatus(Status.APPROVED, "선점");
        this.status = Status.RESERVED;
    }

    /** 계약 결제 완료 시 판매 완료 처리 */
    public void markSold() {
        requireStatus(Status.APPROVED, "판매 완료");
        this.status = Status.SOLD;
    }

    /** 구매 가능 여부 - 승인된 로트만 계약할 수 있다 */
    public boolean isPurchasable() {
        return this.status == Status.APPROVED;
    }

    public void increaseEnrollmentCount() {
        this.enrollmentCount++;
    }

    private void requireStatus(Status expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    expected + " 상태의 로트만 " + action + " 처리할 수 있습니다. 현재 상태: " + this.status);
        }
    }
}
