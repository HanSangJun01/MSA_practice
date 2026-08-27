package com.lecture.enrollment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,   // 수강신청 완료, 결제 대기
        ACTIVE,    // 결제 완료, 수강 활성화
        CANCELLED  // 취소
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }

    public void cancel() {
        if (this.status == Status.PENDING) {
            this.status = Status.CANCELLED;
        }
    }

    /** 결제 실패 후 동일 계약을 다시 시도할 때 기존 행을 재사용한다. */
    public void retry() {
        if (this.status != Status.CANCELLED) {
            throw new IllegalStateException("취소된 계약만 다시 시도할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = Status.PENDING;
    }
}
