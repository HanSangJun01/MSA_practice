package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.kafka.EnrollmentKafkaProducer;
import com.lecture.enrollment.kafka.KafkaEvent;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final UserServiceClient userServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final EnrollmentKafkaProducer kafkaProducer;
    private final EnrollmentWriteService enrollmentWriteService;

    /**
     * 구매·계약 신청 전체 흐름
     * 1. 구매 가능한(APPROVED) 판매 로트인지 확인
     * 2. 중복 계약 확인
     * 3. Enrollment 생성 및 즉시 커밋 (PENDING)
     * 4. 로트 총가격으로 계약금 결제 요청
     */
    public EnrollmentDto.EnrollmentResponse enroll(Long userId, Long courseId) {
        if (!courseServiceClient.existsCourse(courseId)) {
            throw new IllegalArgumentException(
                    "구매할 수 없는 판매 로트입니다 (승인되지 않았거나 이미 판매됨): " + courseId);
        }

        enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .filter(enrollment -> enrollment.getStatus() != Enrollment.Status.CANCELLED)
                .ifPresent(enrollment -> {
                    throw new IllegalArgumentException("이미 계약 신청한 판매 로트입니다");
                });

        // 하드코딩 금액을 쓰지 않고 Course Service 에서 조회한 로트 총가격을 전달한다
        Map<String, Object> lot = courseServiceClient.getCourse(courseId);
        BigDecimal price = toBigDecimal(lot.get("price"));
        if (price == null) {
            throw new IllegalStateException("판매 로트의 총가격을 확인할 수 없습니다: " + courseId);
        }

        Enrollment enrollment = enrollmentWriteService.createPendingEnrollment(userId, courseId);

        try {
            PaymentServiceClient.PaymentResult paymentResult =
                    paymentServiceClient.requestPayment(userId, courseId, price);

            if (paymentResult == null || !"COMPLETED".equals(paymentResult.getStatus())) {
                String status = paymentResult == null ? "응답 없음" : paymentResult.getStatus();
                throw new IllegalStateException("계약금 결제에 실패했습니다. status=" + status);
            }
        } catch (RuntimeException e) {
            enrollmentWriteService.cancelPendingEnrollment(userId, courseId);
            throw e;
        }

        log.info("[EnrollmentService] 계약 신청 완료 (결제 대기) - enrollmentId: {}, amount: {}",
                enrollment.getId(), price);
        return EnrollmentDto.EnrollmentResponse.from(enrollment);
    }

    /**
     * 계약 활성화
     * - Course Service 의 enrollment-count 엔드포인트가 계약 건수 증가와 SOLD 전환을 함께 수행한다
     */
    @Transactional
    public void activateEnrollment(Long userId, Long courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "계약 정보를 찾을 수 없습니다 - userId: " + userId + ", courseId: " + courseId));

        // 중복 이벤트에서도 Course 쪽 멱등 처리를 한 번 확인하되 완료 이벤트는 다시 발행하지 않는다.
        if (enrollment.getStatus() == Enrollment.Status.ACTIVE) {
            courseServiceClient.increaseEnrollmentCount(courseId);
            log.info("[EnrollmentService] 중복 payment.completed 이벤트 무시 - enrollmentId: {}", enrollment.getId());
            return;
        }

        courseServiceClient.increaseEnrollmentCount(courseId);
        enrollment.activate();

        kafkaProducer.publishEnrollmentCompleted(
                KafkaEvent.EnrollmentCompletedEvent.builder()
                        .enrollmentId(enrollment.getId())
                        .userId(userId)
                        .courseId(courseId)
                        .build()
        );

        log.info("[EnrollmentService] 계약 활성화 완료 - enrollmentId: {}", enrollment.getId());
    }

    /**
     * 기업 계약 목록 조회
     * - course-service 에서 판매 로트 상세를 붙여서 반환한다
     * - 내부 상세 조회는 상태 필터가 없어 SOLD 로트도 정상적으로 조립된다
     * - 공급기업명은 내부 응답에 없으므로 instructorId 로 user-service 에서 조달한다
     *   (같은 공급기업이 여러 건이면 목록 조립 한 번 안에서는 한 번만 조회한다)
     */
    public List<EnrollmentDto.EnrollmentResponse> getEnrollmentsByUser(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        Map<Long, String> supplierNameCache = new HashMap<>();

        return enrollments.stream()
                .map(enrollment -> {
                    Map<String, Object> lot = courseServiceClient.getCourse(enrollment.getCourseId());

                    EnrollmentDto.MaterialLotSummary summary = EnrollmentDto.MaterialLotSummary.builder()
                            .id(toLong(lot.get("id")))
                            .title((String) lot.get("title"))
                            .description((String) lot.get("description"))
                            .category(normalizeCategory((String) lot.get("category")))
                            .price(toBigDecimal(lot.get("price")))
                            .quantity(toInteger(lot.get("quantity")))
                            .region((String) lot.get("region"))
                            .components(toComponents(lot.get("components")))
                            .supplierName(resolveSupplierName(lot, supplierNameCache))
                            .contractCount(toInteger(
                                    firstNonNullObject(
                                            lot.get("contractCount"),
                                            lot.get("enrollmentCount"),
                                            lot.get("enrollment_count")
                                    )
                            ))
                            .status((String) lot.get("status"))
                            .thumbnail((String) lot.get("thumbnail"))
                            .build();

                    return EnrollmentDto.EnrollmentResponse.from(enrollment, summary);
                })
                .collect(Collectors.toList());
    }

    /**
     * 구매 이력 조회 - 추천 서비스용
     * - ACTIVE 계약만 대상으로 하며 각 로트의 category·components 를 함께 내려준다
     * - 구매 완료 로트는 SOLD 라 GET /api/courses 에는 없지만
     *   내부 상세 조회(GET /api/courses/internal/{id})는 상태 필터가 없어 조립이 가능하다
     */
    public EnrollmentDto.EnrollmentHistoryResponse getEnrollmentHistory(Long userId) {
        List<Long> activeCourseIds = enrollmentRepository
                .findByUserIdAndStatus(userId, Enrollment.Status.ACTIVE)
                .stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        List<EnrollmentDto.PurchasedLot> purchasedLots = new ArrayList<>();
        for (Long courseId : activeCourseIds) {
            try {
                Map<String, Object> lot = courseServiceClient.getCourse(courseId);
                purchasedLots.add(EnrollmentDto.PurchasedLot.builder()
                        .courseId(courseId)
                        .category((String) lot.get("category"))
                        .components(toComponents(lot.get("components")))
                        .build());
            } catch (Exception e) {
                // 로트 한 건 조회 실패로 추천 입력 전체가 깨지지 않도록 건너뛴다
                log.warn("[EnrollmentService] 구매 이력 성분 조립 실패 - courseId: {}, error: {}",
                        courseId, e.getMessage());
            }
        }

        return EnrollmentDto.EnrollmentHistoryResponse.builder()
                .userId(userId)
                .activeCourseIds(activeCourseIds)
                .purchasedLots(purchasedLots)
                .build();
    }

    /**
     * 공급기업명 조달
     * 내부 로트 응답에 supplierName 이 실려 오면 그대로 쓰고,
     * 없으면 instructorId 로 user-service 를 조회한다 (목록 단위 캐시)
     */
    private String resolveSupplierName(Map<String, Object> lot, Map<Long, String> cache) {
        String fromLot = (String) lot.get("supplierName");
        if (fromLot != null && !fromLot.isBlank()) {
            return fromLot;
        }

        Long instructorId = toLong(lot.get("instructorId"));
        if (instructorId == null) return null;

        if (cache.containsKey(instructorId)) {
            return cache.get(instructorId);
        }
        String companyName = userServiceClient.findCompanyNameOrNull(instructorId);
        cache.put(instructorId, companyName);
        return companyName;
    }

    @SuppressWarnings("unchecked")
    private List<EnrollmentDto.MaterialComponent> toComponents(Object value) {
        if (!(value instanceof List<?> rawList)) return List.of();

        List<EnrollmentDto.MaterialComponent> components = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Map<?, ?> map) {
                Map<String, Object> component = (Map<String, Object>) map;
                components.add(EnrollmentDto.MaterialComponent.builder()
                        .name((String) component.get("name"))
                        .percentage(toBigDecimal(component.get("percentage")))
                        .build());
            }
        }
        return components;
    }

    private String normalizeCategory(String category) {
        if (category == null) return null;

        return switch (category) {
            case "BACKEND" -> "백엔드";
            case "FRONTEND" -> "프론트엔드";
            case "DEVOPS" -> "DevOps";
            case "DATA" -> "데이터";
            case "AI" -> "AI";
            default -> category;
        };
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(value.toString());
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Object firstNonNullObject(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
