package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 기업명으로 사용 (별도 companyName 필드를 만들지 않는다)
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 비즈니스 기업 유형
    // Auth Server 이미지가 같은 users 테이블을 사용하므로 nullable 로 둔다
    @Enumerated(EnumType.STRING)
    @Column(name = "company_type")
    private CompanyType companyType;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 인증 서버 호환 역할 (SUPPLIER/BUYER/INTERMEDIARY 를 여기 넣지 않는다)
    public enum Role {
        STUDENT, INSTRUCTOR
    }

    // 순환거래 기업 유형
    // STUDENT + BUYER = 구매기업, INSTRUCTOR + SUPPLIER = 공급기업, INSTRUCTOR + INTERMEDIARY = 중간 승인기업
    public enum CompanyType {
        BUYER, SUPPLIER, INTERMEDIARY
    }
}
