package com.lecture.course.service;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.entity.MaterialComponent;
import com.lecture.course.exception.ForbiddenException;
import com.lecture.course.exception.NotFoundException;
import com.lecture.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserServiceClient userServiceClient;

    // ===== 공급기업 =====

    /**
     * 판매 로트 등록 (공급기업)
     * - 신규 로트는 항상 PENDING 으로 저장된다
     * - 공급기업명은 목록 조회 N+1 방지를 위해 등록 시점에 비정규화 저장한다
     */
    @Transactional
    public CourseDto.MaterialLotResponse createCourse(CourseDto.CreateRequest request, Long instructorId) {
        // components 는 생략 가능하므로 null 대신 빈 목록으로 저장한다
        List<MaterialComponent> components =
                CourseDto.MaterialComponentDto.toEntityList(request.getComponents());
        if (components == null) {
            components = new ArrayList<>();
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .region(request.getRegion())
                .components(components)
                .instructorId(instructorId)
                .supplierName(userServiceClient.findCompanyNameOrNull(instructorId))
                .status(Course.Status.PENDING)
                .build();

        Course saved = courseRepository.save(course);
        log.info("[CourseService] 판매 로트 등록 - courseId: {}, supplierId: {}, status: {}",
                saved.getId(), instructorId, saved.getStatus());

        return CourseDto.MaterialLotResponse.from(saved);
    }

    /**
     * 공급기업이 등록한 내 판매 로트 목록 (전체 상태)
     */
    public List<CourseDto.MaterialLotResponse> getMyCourses(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .map(CourseDto.MaterialLotResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 공급기업 본인 로트 수정 (소유권 검사)
     * - PENDING 또는 REJECTED 에서만 허용
     * - REJECTED 로트를 수정하면 PENDING 으로 자동 리셋되고 검토 기록이 초기화된다
     */
    @Transactional
    public CourseDto.MaterialLotResponse updateCourse(
            Long id, CourseDto.UpdateRequest request, Long instructorId) {

        Course course = findCourseById(id);
        requireOwner(course, instructorId);

        course.update(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getPrice(),
                request.getQuantity(),
                request.getRegion(),
                CourseDto.MaterialComponentDto.toEntityList(request.getComponents())
        );

        log.info("[CourseService] 판매 로트 수정 - courseId: {}, status: {}", id, course.getStatus());
        return CourseDto.MaterialLotResponse.from(course);
    }

    /**
     * 공급기업 판매 철회 (미계약 로트만, 소유권 검사)
     */
    @Transactional
    public CourseDto.MaterialLotResponse withdrawCourse(Long id, Long instructorId) {
        Course course = findCourseById(id);
        requireOwner(course, instructorId);

        course.withdraw();

        log.info("[CourseService] 판매 로트 철회 - courseId: {}", id);
        return CourseDto.MaterialLotResponse.from(course);
    }

    // ===== 중간 승인기업 =====

    /**
     * 승인 대기 로트 목록 (중간기업)
     */
    public List<CourseDto.MaterialLotResponse> getPendingCourses(Long reviewerId) {
        requireIntermediary(reviewerId);

        return courseRepository.findByStatus(Course.Status.PENDING).stream()
                .map(CourseDto.MaterialLotResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 판매 로트 승인·거절 (중간기업)
     * - reviewerId 는 X-User-Id 로 기록하고 reviewedAt 은 서버에서 생성한다
     */
    @Transactional
    public CourseDto.MaterialLotResponse decideApproval(
            Long id, CourseDto.ApprovalRequest request, Long reviewerId) {

        requireIntermediary(reviewerId);
        Course course = findCourseById(id);

        if (request.getDecision() == CourseDto.ApprovalRequest.Decision.APPROVED) {
            course.approve(reviewerId);
        } else {
            course.reject(reviewerId, request.getRejectionReason());
        }

        log.info("[CourseService] 판매 로트 검토 완료 - courseId: {}, decision: {}, reviewerId: {}",
                id, request.getDecision(), reviewerId);

        return CourseDto.MaterialLotResponse.from(course);
    }

    /**
     * 원료 설명 보정 (중간기업)
     * - 소유권이 아니라 companyType=INTERMEDIARY 역할로 인가한다
     * - 검토 행위의 일부이지 승인 절차가 아니므로 상태·검토 기록을 건드리지 않는다
     */
    @Transactional
    public CourseDto.MaterialLotResponse updateDescription(
            Long id, CourseDto.DescriptionUpdateRequest request, Long reviewerId) {

        requireIntermediary(reviewerId);
        Course course = findCourseById(id);

        course.updateDescription(request.getDescription());

        log.info("[CourseService] 원료 설명 보정 - courseId: {}, reviewerId: {}, status 유지: {}",
                id, reviewerId, course.getStatus());

        return CourseDto.MaterialLotResponse.from(course);
    }

    // ===== 구매기업 =====

    /**
     * 판매 로트 상세 조회
     * - 한 번이라도 공개된 로트(APPROVED, SOLD)만 노출한다.
     *   그 외(PENDING, REJECTED, WITHDRAWN)는 존재 자체를 숨기기 위해 404 로 응답한다
     * - SOLD 를 포함하는 이유: APPROVED 시절 이미 공개됐던 로트이고,
     *   막아두면 구매기업이 자기가 계약한 로트를 상세 조회할 수 없다.
     *   course-service 는 계약 정보를 모르므로 구매자 여부를 직접 판별할 수 없다
     * - 공급기업 본인은 자기 로트를 상태와 무관하게 조회할 수 있다 (requesterId 는 선택)
     */
    public CourseDto.MaterialLotResponse getCourse(Long id, Long requesterId) {
        Course course = findCourseById(id);

        boolean owner = requesterId != null && requesterId.equals(course.getInstructorId());
        if (!owner && !isPubliclyVisible(course)) {
            throw new NotFoundException("판매 로트를 찾을 수 없습니다: " + id);
        }

        return CourseDto.MaterialLotResponse.from(course);
    }

    /** 상세 조회로 공개할 수 있는 상태인지 - 목록·추천 후보는 여전히 APPROVED 만 대상이다 */
    private boolean isPubliclyVisible(Course course) {
        return course.getStatus() == Course.Status.APPROVED
                || course.getStatus() == Course.Status.SOLD;
    }

    /**
     * 승인된 판매 로트 전체 목록
     */
    public List<CourseDto.MaterialLotResponse> getAllCourses() {
        return courseRepository.findByStatus(Course.Status.APPROVED).stream()
                .map(CourseDto.MaterialLotResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 승인 판매 로트 조회
     */
    public List<CourseDto.MaterialLotResponse> getCoursesByCategory(Course.Category category) {
        return courseRepository.findByCategoryAndStatus(category, Course.Status.APPROVED).stream()
                .map(CourseDto.MaterialLotResponse::from)
                .collect(Collectors.toList());
    }

    // ===== 내부 호출 =====

    /**
     * 구매 가능한 판매 로트 존재 여부 (Enrollment Service 호출)
     * - 단순 존재 여부가 아니라 APPROVED 여부를 확인한다
     */
    public boolean existsCourse(Long id) {
        return courseRepository.existsByIdAndStatus(id, Course.Status.APPROVED);
    }

    /**
     * 판매 로트 상세 조회 (내부) - 상태 필터 없음
     * SOLD 로트도 반환하므로 구매 이력 성분 조립에 사용할 수 있다
     */
    public CourseDto.CourseInternalResponse getCourseInternal(Long id) {
        return CourseDto.CourseInternalResponse.from(findCourseById(id));
    }

    /**
     * 계약 완료 건수 증가 및 판매 로트 SOLD 처리 (Enrollment Service 호출)
     * - 새 엔드포인트를 만들지 않고 이 하나가 두 가지를 함께 수행한다
     */
    @Transactional
    public void increaseEnrollmentCount(Long courseId) {
        Course course = findCourseById(courseId);

        // Kafka 는 같은 이벤트를 다시 전달할 수 있으므로 이미 완료된 처리는 성공으로 간주한다.
        if (course.getStatus() == Course.Status.SOLD) {
            log.info("[CourseService] 중복 계약 완료 이벤트 무시 - courseId: {}", courseId);
            return;
        }

        if (course.getStatus() != Course.Status.APPROVED) {
            throw new IllegalStateException(
                    "APPROVED 상태의 로트만 계약 완료 처리할 수 있습니다. 현재 상태: " + course.getStatus());
        }

        // 상태 전이를 먼저 검증한 뒤 계약 건수를 올려 0 또는 1을 유지한다.
        course.markSold();
        course.increaseEnrollmentCount();

        log.info("[CourseService] 계약 완료 처리 - courseId: {}, contractCount: {}, status: {}",
                courseId, course.getEnrollmentCount(), course.getStatus());
    }

    /**
     * 추천 후보 조회 (내부)
     * - category 는 선택이다. 생략하면 APPROVED 전체를 반환한다
     * - 성분 매칭·정렬·상위 N건 선별은 Recommend Service 가 담당한다
     */
    public List<CourseDto.CourseInternalResponse> getRecommendCourses(Course.Category category) {
        List<Course> courses = (category == null)
                ? courseRepository.findByStatus(Course.Status.APPROVED)
                : courseRepository.findByCategoryAndStatus(category, Course.Status.APPROVED);

        return courses.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(CourseDto.CourseInternalResponse::from)
                .collect(Collectors.toList());
    }

    // ===== 공통 =====

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("판매 로트를 찾을 수 없습니다: " + id));
    }

    /** 소유권 검사 - 본인이 등록한 로트인지 확인 */
    private void requireOwner(Course course, Long instructorId) {
        if (!course.getInstructorId().equals(instructorId)) {
            throw new ForbiddenException("본인이 등록한 판매 로트만 수정·철회할 수 있습니다");
        }
    }

    /** 역할 검사 - companyType 은 JWT 에 없으므로 user-service 조회가 유일한 경로다 */
    private void requireIntermediary(Long userId) {
        if (!userServiceClient.getUser(userId).isIntermediary()) {
            throw new ForbiddenException("중간 승인기업(companyType=INTERMEDIARY)만 수행할 수 있습니다");
        }
    }
}
