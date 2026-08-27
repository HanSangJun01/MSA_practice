package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "판매 로트", description = """
        순환원료 판매 로트. `Course` 한 건은 재사용 가능한 상품 마스터가 아니라
        **판매 가능한 원료 로트 한 건**이며, 한 구매기업이 전체를 구매한다.

        ### 상태 전이

        표에 없는 전이는 400으로 거절한다.

        | From | To | 트리거 |
        |---|---|---|
        | (신규) | `PENDING` | 공급기업 로트 등록 |
        | `PENDING` | `APPROVED` | 중간기업 승인 |
        | `PENDING` | `REJECTED` | 중간기업 거절 |
        | `REJECTED` | `PENDING` | 공급기업이 로트 수정 시 자동 리셋 |
        | `APPROVED` | `SOLD` | 계약 결제 완료 |
        | `PENDING` / `APPROVED` | `WITHDRAWN` | 공급기업 판매 철회 (미계약 상태에서만) |

        `RESERVED`는 MVP에서 사용하지 않는다. 결제가 동기 시뮬레이션이라 선점 구간이 없다.

        ### 노출 규칙

        - 목록·카테고리 조회·추천 후보: `APPROVED`만
        - 상세 조회: `APPROVED` 또는 `SOLD` (한 번이라도 공개됐던 로트).
          `PENDING`·`REJECTED`·`WITHDRAWN`은 존재 자체를 숨기려고 404를 반환한다.
          단 공급기업 본인은 자기 로트를 상태와 무관하게 조회할 수 있다.

        ### 경로와 필드명

        Gateway 호환을 위해 `/api/courses` 경로를 유지한다(`/api/materials`로 바꾸지 않는다).
        내부 필드명도 기존 계약을 유지하며 외부 응답에서만 용어를 바꾼다.

        | 의미 | 외부 응답 | 내부(`/internal/**`) |
        |---|---|---|
        | 공급기업 ID | `supplierId` | `instructorId` |
        | 계약 완료 건수 | `contractCount` | `enrollmentCount` |
        | 공급기업명 | `supplierName` | 없음 |
        """)
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * POST /courses - 판매 로트 등록 (공급기업)
     * 신규 로트는 항상 PENDING 으로 저장된다
     */
    @Operation(summary = "판매 로트 등록 (공급기업)", description = """
            공급기업이 순환원료 판매 로트를 등록한다. **신규 로트는 항상 `PENDING`으로 저장**되며
            중간기업 승인 전에는 구매기업 목록이나 추천 후보에 나오지 않는다.

            `price`는 로트 전체의 총가격이고 `quantity`는 로트 수량이다.
            단위(`unit`)는 별도 필드로 두지 않으므로 필요하면 로트명이나 설명에 자연어로 표기한다.

            `components[].name`은 **enum으로 강제**한다. 자유 문자열을 허용하면
            `"리튬"` / `"LITHIUM"` / `"Li"`가 섞여 추천의 성분 교집합이 잡히지 않는다.

            공급기업명은 목록 조회 N+1을 피해 등록 시점에 user-service에서 조달해 저장한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공. `status`는 항상 `PENDING`"),
            @ApiResponse(responseCode = "400",
                    description = "필수 항목 누락, enum 밖의 카테고리·성분명, 또는 `X-User-Id` 헤더 누락")
    })
    @PostMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> createCourse(
            @Valid @RequestBody CourseDto.CreateRequest request,
            @Parameter(description = "Gateway가 주입하는 공급기업 ID", required = true, example = "2")
            @RequestHeader("X-User-Id") Long instructorId) {

        CourseDto.MaterialLotResponse response = courseService.createCourse(request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseDto.ApiResponse.success(response));
    }

    /**
     * GET /courses - 승인된 판매 로트 전체 목록
     */
    @Operation(summary = "승인된 판매 로트 전체 조회", description = """
            구매기업에게 노출할 로트 목록이다. **`APPROVED`만** 반환한다.

            계약이 완료된 로트는 `SOLD`가 되어 이 목록에서 사라진다.
            """)
    @GetMapping
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getAllCourses() {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getAllCourses())
        );
    }

    /**
     * GET /courses/my - 공급기업 내 등록 로트 조회 (전체 상태)
     */
    @Operation(summary = "내 등록 로트 조회 (공급기업)", description = """
            공급기업이 자신이 등록한 로트를 **전체 상태**로 조회한다.
            거절 사유(`rejectionReason`)와 검토 기록도 함께 확인할 수 있다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (전체 상태)"),
            @ApiResponse(responseCode = "400", description = "`X-User-Id` 헤더 누락")
    })
    @GetMapping("/my")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getMyCourses(
            @Parameter(description = "Gateway가 주입하는 공급기업 ID", required = true, example = "2")
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getMyCourses(instructorId))
        );
    }

    /**
     * GET /courses/approval/pending - 승인 대기 로트 목록 (중간기업)
     */
    @Operation(summary = "승인 대기 로트 조회 (중간기업)", description = """
            중간기업이 검토해야 할 `PENDING` 로트 목록이다.

            `companyType`은 JWT 클레임에 없으므로 `X-User-Id`로 user-service를 조회해
            `INTERMEDIARY` 여부를 확인한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (PENDING 로트 목록)"),
            @ApiResponse(responseCode = "400", description = "`X-User-Id` 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "중간기업(`companyType=INTERMEDIARY`)이 아님")
    })
    @GetMapping("/approval/pending")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getPendingCourses(
            @Parameter(description = "Gateway가 주입하는 중간기업 ID. reviewerId로 기록된다", required = true, example = "7")
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getPendingCourses(reviewerId))
        );
    }

    /**
     * GET /courses/category/{category} - 카테고리별 승인 판매 로트 조회
     */
    @Operation(summary = "카테고리별 승인 로트 조회",
            description = "지정한 산업 부산물 카테고리의 `APPROVED` 로트만 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (APPROVED만)"),
            @ApiResponse(responseCode = "400", description = "허용되지 않은 카테고리 값")
    })
    @GetMapping("/category/{category}")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.MaterialLotResponse>>> getCoursesByCategory(
            @Parameter(description = "산업 부산물 카테고리", example = "BATTERY")
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
    @Operation(summary = "판매 로트 상세 조회", description = """
            한 번이라도 공개된 로트(`APPROVED`, `SOLD`)를 조회한다.

            `SOLD`를 포함하는 이유는 구매기업이 자신이 계약한 로트를 계속 볼 수 있어야 하기 때문이다.
            course-service는 계약 정보를 모르므로 요청자가 구매자인지 직접 판별할 수 없다.

            `PENDING`·`REJECTED`·`WITHDRAWN`은 존재 자체를 숨기려고 404를 반환한다.
            단 공급기업 본인(`X-User-Id`가 `instructorId`와 일치)은 상태와 무관하게 조회할 수 있다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "없는 로트이거나, 아직 공개된 적 없는 로트")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> getCourse(
            @Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id,
            @Parameter(description = "선택. 공급기업 본인이 자기 로트를 상태와 무관하게 조회할 때 사용된다")
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getCourse(id, requesterId))
        );
    }

    /**
     * PATCH /courses/{id} - 공급기업 본인 로트 수정 (소유권 검사)
     * PENDING·REJECTED 에서만 허용하며, REJECTED 는 PENDING 으로 자동 리셋된다
     */
    @Operation(summary = "판매 로트 수정 (공급기업 본인)", description = """
            공급기업이 자기 로트를 수정한다. **소유권 검사**(`instructorId == X-User-Id`)를 한다.

            `PENDING` 또는 `REJECTED`에서만 허용하며, null로 보낸 필드는 변경하지 않는다.

            **`REJECTED` 로트를 수정하면 `PENDING`으로 자동 리셋**되고
            `reviewerId`·`reviewedAt`·`rejectionReason`이 초기화되어 재승인 대상이 된다.

            중간기업의 설명 보정은 이 API가 아니라 `PATCH /{id}/description`이다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공. REJECTED였다면 PENDING으로 리셋된다"),
            @ApiResponse(responseCode = "400", description = "`PENDING`·`REJECTED`가 아닌 상태이거나 헤더 누락"),
            @ApiResponse(responseCode = "403", description = "본인이 등록한 로트가 아님"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> updateCourse(
            @Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id,
            @Valid @RequestBody CourseDto.UpdateRequest request,
            @Parameter(description = "Gateway가 주입하는 공급기업 ID", required = true, example = "2")
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.updateCourse(id, request, instructorId))
        );
    }

    /**
     * PATCH /courses/{id}/withdraw - 공급기업 판매 철회 (미계약 로트만)
     */
    @Operation(summary = "판매 철회 (공급기업 본인)", description = """
            공급기업이 아직 계약되지 않은 자기 로트를 `WITHDRAWN`으로 내린다.

            `PENDING` 또는 `APPROVED`에서만 가능하며, 이미 계약된 로트는 철회할 수 없다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "철회 성공"),
            @ApiResponse(responseCode = "400", description = "이미 계약된 로트이거나 철회 가능한 상태가 아님"),
            @ApiResponse(responseCode = "403", description = "본인이 등록한 로트가 아님"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> withdrawCourse(
            @Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id,
            @Parameter(description = "Gateway가 주입하는 공급기업 ID", required = true, example = "2")
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.withdrawCourse(id, instructorId))
        );
    }

    /**
     * PATCH /courses/{id}/approval - 판매 로트 승인·거절 (중간기업)
     * reviewerId 는 본문이 아니라 X-User-Id 로 기록한다
     */
    @Operation(summary = "판매 로트 승인·거절 (중간기업)", description = """
            중간기업이 등록 정보와 거래 적합성을 검토해 승인하거나 거절한다.
            품질 인증이 아니라 **플랫폼 노출 승인** 절차다.

            `PENDING` 로트만 대상이다. 승인하면 `APPROVED`가 되어 구매기업 목록과 추천에 노출되고,
            거절하면 `REJECTED`가 되며 `rejectionReason`이 저장된다.

            `reviewerId`는 요청 본문에서 받지 않고 `X-User-Id`로 기록하며 `reviewedAt`은 서버가 생성한다.

            인가는 `companyType=INTERMEDIARY`로 판정한다. JWT에 `companyType` 클레임이 없어
            user-service 조회가 유일한 확인 수단이다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검토 완료. reviewerId와 reviewedAt이 기록된다"),
            @ApiResponse(responseCode = "400", description = "`PENDING`이 아닌 로트이거나 `decision` 누락"),
            @ApiResponse(responseCode = "403", description = "중간기업(`companyType=INTERMEDIARY`)이 아님"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @PatchMapping("/{id}/approval")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> decideApproval(
            @Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id,
            @Valid @RequestBody CourseDto.ApprovalRequest request,
            @Parameter(description = "Gateway가 주입하는 중간기업 ID. reviewerId로 기록된다", required = true, example = "7")
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.decideApproval(id, request, reviewerId))
        );
    }

    /**
     * PATCH /courses/{id}/description - 원료 설명 보정 (중간기업)
     * 소유권이 아니라 companyType=INTERMEDIARY 역할로 인가하며 상태를 바꾸지 않는다
     */
    @Operation(summary = "원료 설명 보정 (중간기업)", description = """
            중간기업이 검토 과정에서 원료 설명을 보완한다. `description`만 바꾼다.

            **승인 절차가 아니라 검토 행위의 일부**이므로
            `status`·`reviewerId`·`reviewedAt`·`rejectionReason`을 건드리지 않는다.
            `PENDING` 로트는 `PENDING`으로, `APPROVED` 로트는 `APPROVED`로 남는다.
            승인 여부 변경은 `PATCH /{id}/approval`로만 한다.

            인가 기준이 `PATCH /{id}`와 다르다. 이쪽은 **소유권이 아니라 역할**(`companyType=INTERMEDIARY`)로
            판정하므로, 공급기업이 자기 로트에 호출해도 403이다. 전체 수정은 `PATCH /{id}`를 쓴다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보정 성공. 상태와 검토 기록은 변하지 않는다"),
            @ApiResponse(responseCode = "400", description = "`PENDING`·`APPROVED`가 아닌 상태이거나 `description` 누락"),
            @ApiResponse(responseCode = "403", description = "중간기업이 아님 (공급기업·구매기업 모두 포함)"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @PatchMapping("/{id}/description")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.MaterialLotResponse>> updateDescription(
            @Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id,
            @Valid @RequestBody CourseDto.DescriptionUpdateRequest request,
            @Parameter(description = "Gateway가 주입하는 중간기업 ID. reviewerId로 기록된다", required = true, example = "7")
            @RequestHeader("X-User-Id") Long reviewerId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.updateDescription(id, request, reviewerId))
        );
    }

    /**
     * GET /courses/internal/exists/{id} - 구매 가능한 로트 존재 여부 (Enrollment Service 호출)
     * 단순 존재 여부가 아니라 APPROVED 여부를 확인한다
     */
    @Operation(summary = "[내부] 구매 가능한 로트인지 확인", description = """
            Enrollment Service가 계약 신청 전에 호출한다. `true`/`false`를 래퍼 없이 반환한다.

            단순 존재 여부가 아니라 **`APPROVED` 여부**를 확인한다.
            따라서 이미 판매된(`SOLD`) 로트는 `false`가 되어 중복 계약이 차단된다.
            """)
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsCourse(@Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id) {
        return ResponseEntity.ok(courseService.existsCourse(id));
    }

    /**
     * GET /courses/internal/{id} - 판매 로트 상세 조회 (내부, 상태 필터 없음)
     * - 계약 목록·구매 이력 성분 조립 시 사용하며 SOLD 로트도 반환한다
     * - 래퍼 없이 CourseInternalResponse 만 직접 반환
     */
    @Operation(summary = "[내부] 판매 로트 상세 조회", description = """
            서비스 간 호출용. 래퍼 없이 `CourseInternalResponse`를 바로 반환한다.

            **상태 필터가 없어 `SOLD` 로트도 반환한다.** 구매 완료 로트는
            `GET /api/courses`에서 사라지므로, 계약 목록과 구매 이력 성분 조립은 이 경로로 한다.

            외부 응답과 달리 `instructorId`·`enrollmentCount`를 쓰고 `supplierName`은 없다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (SOLD 로트 포함)"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @GetMapping("/internal/{id}")
    public ResponseEntity<CourseDto.CourseInternalResponse> getCourseInternal(@Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseInternal(id));
    }

    /**
     * POST /courses/internal/{id}/enrollment-count - 계약 완료 건수 증가 및 SOLD 전환 (Enrollment Service 호출)
     */
    @Operation(summary = "[내부] 계약 완료 처리 (건수 증가 + SOLD 전환)", description = """
            Enrollment Service가 결제 완료 이벤트를 처리할 때 호출한다.
            **계약 건수 증가와 `SOLD` 전환을 이 하나가 함께 수행**하므로 별도 엔드포인트가 없다.

            멱등하다. `APPROVED`일 때만 건수를 1로 올리고 `SOLD`로 바꾸며,
            이미 `SOLD`면 Kafka 중복 이벤트로 보고 아무 작업 없이 성공 처리한다.
            그 외 상태에서는 400으로 거절한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 완료 (중복 이벤트로 인한 무시 포함)"),
            @ApiResponse(responseCode = "400", description = "`APPROVED`도 `SOLD`도 아닌 로트"),
            @ApiResponse(responseCode = "404", description = "없는 로트")
    })
    @PostMapping("/internal/{id}/enrollment-count")
    public ResponseEntity<Void> increaseEnrollmentCount(@Parameter(description = "판매 로트 ID", example = "10") @PathVariable Long id) {
        courseService.increaseEnrollmentCount(id);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /courses/internal/recommend - 추천 후보 조회 (APPROVED 로트)
     * category 는 선택이다. 생략하면 APPROVED 전체를 반환한다.
     * excludeIds 는 사용하지 않는다 - 계약된 로트는 SOLD 라 애초에 후보에 없다.
     */
    @Operation(summary = "[내부] 추천 후보 조회", description = """
            Recommend Service가 쓰는 추천 후보 목록이다. 래퍼 없이 반환하며 **`APPROVED`만** 담긴다.

            `category`는 **선택**이다. 생략하면 `APPROVED` 전체를 최신순으로 반환한다.
            성분 기반이든 카테고리 기반이든 후보 전체를 받을 수 있어야 하기 때문이다.

            성분 매칭·정렬·상위 N건 선별은 Recommend Service가 담당하고 여기서는 필터링하지 않는다.
            응답에는 매칭에 필요한 `components`, `region`, `quantity`, `price`가 포함된다.

            `excludeIds`는 없다. 계약된 로트는 `SOLD`가 되어 애초에 후보에 들어오지 않으므로
            상태 필터가 그 역할을 대신한다.
            """)
    @GetMapping("/internal/recommend")
    public ResponseEntity<List<CourseDto.CourseInternalResponse>> getRecommendCourses(
            @Parameter(description = "선택. 생략하면 APPROVED 전체를 반환한다", example = "BATTERY")
            @RequestParam(required = false) Course.Category category) {
        return ResponseEntity.ok(courseService.getRecommendCourses(category));
    }
}
