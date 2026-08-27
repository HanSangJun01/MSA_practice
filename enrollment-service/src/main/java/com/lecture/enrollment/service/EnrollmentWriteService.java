package com.lecture.enrollment.service;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentWriteService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * 반드시 독립 트랜잭션으로 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Enrollment createPendingEnrollment(Long userId, Long courseId) {

        Enrollment existing = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (existing != null) {
            existing.retry();
            log.info("[EnrollmentWriteService] CANCELLED enrollment 재시도 - enrollmentId: {}, userId: {}, courseId: {}",
                    existing.getId(), userId, courseId);
            return existing;
        }

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .build()
        );

        log.info("[EnrollmentWriteService] PENDING enrollment 생성 완료 - enrollmentId: {}, userId: {}, courseId: {}",
                enrollment.getId(), userId, courseId);

        return enrollment;
    }

    /** 결제 요청이 실패하면 PENDING 계약을 취소해 다음 요청에서 재시도할 수 있게 한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelPendingEnrollment(Long userId, Long courseId) {
        enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresent(enrollment -> {
                    if (enrollment.getStatus() == Enrollment.Status.PENDING) {
                        enrollment.cancel();
                        log.warn("[EnrollmentWriteService] 결제 실패 계약 취소 - enrollmentId: {}, userId: {}, courseId: {}",
                                enrollment.getId(), userId, courseId);
                    }
                });
    }
}
