-- 산업 부산물 순환거래 플랫폼 초기 DDL
-- Spring JPA ddl-auto: update 로도 생성되지만
-- 명시적 DDL로 테이블 선후 관계를 문서화

-- 기업 (구매기업 / 공급기업 / 중간 승인기업)
-- name 컬럼을 기업명으로 사용한다 (별도 company_name 없음)
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    email        VARCHAR(255)    NOT NULL UNIQUE,
    password     VARCHAR(255)    NOT NULL,
    name         VARCHAR(100)    NOT NULL COMMENT '기업명',
    role         VARCHAR(20)     NOT NULL COMMENT '인증 서버 호환 역할: STUDENT | INSTRUCTOR',
    -- Auth Server 이미지가 같은 테이블을 사용하므로 nullable 로 둔다
    company_type VARCHAR(20)     NULL COMMENT '기업 유형: BUYER | SUPPLIER | INTERMEDIARY',
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 공급기업이 순환원료 판매 로트 등록 (instructor_id → users.id)
-- Course 한 건 = 판매 가능한 원료 로트 한 건 (부분 구매 없음)
CREATE TABLE IF NOT EXISTS courses (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    title            VARCHAR(255)    NOT NULL COMMENT '판매 로트명',
    description      TEXT            COMMENT '원료 설명',
    category         VARCHAR(50)     NOT NULL COMMENT '산업 부산물 카테고리: METAL|PLASTIC|BATTERY|ELECTRONIC|CHEMICAL|CONSTRUCTION|TEXTILE|OTHER',
    price            DECIMAL(18,2)   NOT NULL COMMENT '로트 총가격',
    quantity         INT             COMMENT '로트 수량 (unit 은 MVP 범위 밖 - 로트명/설명에 자연어 표기)',
    region           VARCHAR(100)    COMMENT '공급 지역',
    instructor_id    BIGINT          NOT NULL COMMENT '공급기업 ID',
    supplier_name    VARCHAR(100)    COMMENT '공급기업명 (목록 조회 N+1 방지용 비정규화 저장)',
    enrollment_count INT             NOT NULL DEFAULT 0 COMMENT '계약 완료 건수 (로트 MVP 에서는 0 또는 1)',
    status           VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '로트 상태: PENDING|APPROVED|REJECTED|RESERVED|SOLD|WITHDRAWN (RESERVED 는 MVP 미사용)',
    reviewer_id      BIGINT          COMMENT '검토 중간기업 ID',
    reviewed_at      DATETIME(6)     COMMENT '검토 일시',
    rejection_reason VARCHAR(500)    COMMENT '거절 사유',
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    FOREIGN KEY (instructor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 로트 성분·함량 (@ElementCollection)
-- 성분명은 enum 으로 강제한다. 자유 문자열이면 추천 성분 교집합이 잡히지 않는다.
CREATE TABLE IF NOT EXISTS course_components (
    course_id      BIGINT          NOT NULL,
    component_name VARCHAR(30)     NOT NULL COMMENT 'LITHIUM|COBALT|NICKEL|COPPER|ALUMINUM|IRON|MANGANESE|PET|PP|PE|PVC|OTHER',
    percentage     DECIMAL(5,2)    NOT NULL COMMENT '함량 0~100',
    FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 구매기업이 구매·계약 신청 (user_id → users.id, course_id → courses.id)
CREATE TABLE IF NOT EXISTS enrollments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL COMMENT '구매기업 ID',
    course_id   BIGINT      NOT NULL COMMENT '판매 로트 ID',
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | ACTIVE | CANCELLED',
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_course (user_id, course_id),
    FOREIGN KEY (user_id)   REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 계약 확정 후 계약금 결제 시뮬레이션 (user_id → users.id, course_id → courses.id)
CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '구매기업 ID',
    course_id       BIGINT          NOT NULL COMMENT '판매 로트 ID',
    amount          DECIMAL(18,2)   NOT NULL COMMENT '로트 총가격',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | FAILED | CANCELLED',
    transaction_id  VARCHAR(255)    UNIQUE,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    PRIMARY KEY (id),
    FOREIGN KEY (user_id)   REFERENCES users(id),
    FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
