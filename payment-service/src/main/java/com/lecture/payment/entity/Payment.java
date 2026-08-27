package com.lecture.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구매기업 ID (내부 필드명은 기존 계약을 유지한다)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 판매 로트 ID (내부 필드명은 기존 계약을 유지한다)
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    // 로트 총가격 (B2B 금액이므로 DECIMAL(18,2))
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    // 외부 PG사 거래 ID (실습에서는 UUID로 대체)
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // 결제 대기
        COMPLETED,  // 결제 완료
        FAILED,     // 결제 실패
        CANCELLED   // 취소
    }

    public void complete(String transactionId) {
        this.status = Status.COMPLETED;
        this.transactionId = transactionId;
    }

    public void fail() {
        this.status = Status.FAILED;
    }
}
