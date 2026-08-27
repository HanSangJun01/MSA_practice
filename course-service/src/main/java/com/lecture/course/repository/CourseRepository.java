package com.lecture.course.repository;

import com.lecture.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // 카테고리별 판매 로트 조회 (구매기업 목록·추천 후보)
    List<Course> findByCategoryAndStatus(Course.Category category, Course.Status status);

    // 공급기업별 내 로트 조회 (전체 상태)
    List<Course> findByInstructorId(Long instructorId);

    // 상태별 전체 조회 (APPROVED 목록 / PENDING 승인 대기 목록)
    List<Course> findByStatus(Course.Status status);

    // 구매 가능한 로트 존재 여부 (Enrollment Service 내부 호출용)
    boolean existsByIdAndStatus(Long id, Course.Status status);

    // 카테고리별 + 특정 ID 제외 조회
    // excludeIds 는 현재 사용하지 않는다 - 계약된 로트는 SOLD 라 애초에 APPROVED 후보에 없다
    List<Course> findByCategoryAndStatusAndIdNotIn(
            Course.Category category,
            Course.Status status,
            List<Long> excludeIds
    );
}
