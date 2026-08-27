package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 판매 로트 API
 *
 * Gateway 호환을 위해 /api/courses 경로를 유지한다 (/api/materials 로 바꾸지 않는다).
 * X-User-Id 는 Gateway 의 JwtAuthenticationFilter 가 주입한다.
 * 서비스를 8082 로 직접 호출하면 이 헤더가 없으므로 400 으로 거절된다.
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * POST /courses - 판매 로트 등록 (공급기업)
     * 신규 로트는 항상 PENDING 으로 저장된다
     */
    @PostMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> createCourse(
            @Valid @RequestBody CourseDto.CreateRequest request,
            @RequestHeader("X-User-Id") Long instructorId) {

        CourseDto.MaterialLotResponse response = courseService.createCourse(request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseDto.ApiResponse.success(response));
    }

    /**
     * GET /courses - 승인된 판매 로트 전체 목록
     */
    @GetMapping
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getAllCourses() {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getAllCourses())
        );
    }

    /**
     * GET /courses/my - 공급기업 내 등록 로트 조회 (전체 상태)
     */
    @GetMapping("/my")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getMyCourses(
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getMyCourses(instructorId))
        );
    }

    /**
     * GET /courses/approval/pending - 승인 대기 로트 목록 (중간기업)
     */
    @GetMapping("/approval/pending")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getPendingCourses(
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getPendingCourses(reviewerId))
        );
    }

    /**
     * GET /courses/category/{category} - 카테고리별 승인 판매 로트 조회
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getCoursesByCategory(
            @PathVariable Course.Category category) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getCoursesByCategory(category))
        );
    }

    /**
     * GET /courses/{id} - 판매 로트 상세 조회
     * 구매기업에게는 APPROVED 로트만 노출한다. 비승인 로트는 404 로 존재 자체를 숨긴다.
     * 공급기업 본인은 X-User-Id 로 자기 로트를 상태와 무관하게 조회할 수 있다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> getCourse(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getCourse(id, requesterId))
        );
    }

    /**
     * PATCH /courses/{id} - 공급기업 본인 로트 수정 (소유권 검사)
     * PENDING·REJECTED 에서만 허용하며, REJECTED 는 PENDING 으로 자동 리셋된다
     */
    @PatchMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto.UpdateRequest request,
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.updateCourse(id, request, instructorId))
        );
    }

    /**
     * PATCH /courses/{id}/withdraw - 공급기업 판매 철회 (미계약 로트만)
     */
    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> withdrawCourse(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.withdrawCourse(id, instructorId))
        );
    }

    /**
     * PATCH /courses/{id}/approval - 판매 로트 승인·거절 (중간기업)
     * reviewerId 는 본문이 아니라 X-User-Id 로 기록한다
     */
    @PatchMapping("/{id}/approval")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> decideApproval(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto.ApprovalRequest request,
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.decideApproval(id, request, reviewerId))
        );
    }

    /**
     * PATCH /courses/{id}/description - 원료 설명 보정 (중간기업)
     * 소유권이 아니라 companyType=INTERMEDIARY 역할로 인가하며 상태를 바꾸지 않는다
     */
    @PatchMapping("/{id}/description")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> updateDescription(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto.DescriptionUpdateRequest request,
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.updateDescription(id, request, reviewerId))
        );
    }

    /**
     * GET /courses/internal/exists/{id} - 구매 가능한 로트 존재 여부 (Enrollment Service 호출)
     * 단순 존재 여부가 아니라 APPROVED 여부를 확인한다
     */
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.existsCourse(id));
    }

    /**
     * GET /courses/internal/{id} - 판매 로트 상세 조회 (내부, 상태 필터 없음)
     * - 계약 목록·구매 이력 성분 조립 시 사용하며 SOLD 로트도 반환한다
     * - 래퍼 없이 CourseInternalResponse 만 직접 반환
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<CourseDto.CourseInternalResponse> getCourseInternal(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseInternal(id));
    }

    /**
     * POST /courses/internal/{id}/enrollment-count - 계약 완료 건수 증가 및 SOLD 전환 (Enrollment Service 호출)
     */
    @PostMapping("/internal/{id}/enrollment-count")
    public ResponseEntity<Void> increaseEnrollmentCount(@PathVariable Long id) {
        courseService.increaseEnrollmentCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /courses/internal/recommend - 추천 후보 조회 (APPROVED 로트)
     * category 는 선택이다. 생략하면 APPROVED 전체를 반환한다.
     * excludeIds 는 사용하지 않는다 - 계약된 로트는 SOLD 라 애초에 후보에 없다.
     */
    @GetMapping("/internal/recommend")
    public ResponseEntity<List<CourseDto.CourseInternalResponse>> getRecommendCourses(
            @RequestParam(required = false) Course.Category category) {
        return ResponseEntity.ok(courseService.getRecommendCourses(category));
    }
}
