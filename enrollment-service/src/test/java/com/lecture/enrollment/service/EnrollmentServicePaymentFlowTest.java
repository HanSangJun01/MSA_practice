package com.lecture.enrollment.service;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.kafka.EnrollmentKafkaProducer;
import com.lecture.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EnrollmentServicePaymentFlowTest {

    private final EnrollmentRepository enrollmentRepository = mock(EnrollmentRepository.class);
    private final CourseServiceClient courseServiceClient = mock(CourseServiceClient.class);
    private final UserServiceClient userServiceClient = mock(UserServiceClient.class);
    private final PaymentServiceClient paymentServiceClient = mock(PaymentServiceClient.class);
    private final EnrollmentKafkaProducer kafkaProducer = mock(EnrollmentKafkaProducer.class);
    private final EnrollmentWriteService enrollmentWriteService = mock(EnrollmentWriteService.class);

    private final EnrollmentService enrollmentService = new EnrollmentService(
            enrollmentRepository,
            courseServiceClient,
            userServiceClient,
            paymentServiceClient,
            kafkaProducer,
            enrollmentWriteService
    );

    @Test
    void failedPaymentCancelsPendingEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(3L)
                .courseId(10L)
                .build();
        PaymentServiceClient.PaymentResult failedResult = mock(PaymentServiceClient.PaymentResult.class);

        when(courseServiceClient.existsCourse(10L)).thenReturn(true);
        when(enrollmentRepository.findByUserIdAndCourseId(3L, 10L)).thenReturn(Optional.empty());
        when(courseServiceClient.getCourse(10L)).thenReturn(Map.of("price", new BigDecimal("12000000")));
        when(enrollmentWriteService.createPendingEnrollment(3L, 10L)).thenReturn(enrollment);
        when(paymentServiceClient.requestPayment(3L, 10L, new BigDecimal("12000000")))
                .thenReturn(failedResult);
        when(failedResult.getStatus()).thenReturn("FAILED");

        assertThrows(IllegalStateException.class, () -> enrollmentService.enroll(3L, 10L));
        verify(enrollmentWriteService).cancelPendingEnrollment(3L, 10L);
    }

    @Test
    void courseCompletionFailureDoesNotActivateEnrollment() {
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(3L)
                .courseId(10L)
                .build();
        when(enrollmentRepository.findByUserIdAndCourseId(3L, 10L))
                .thenReturn(Optional.of(enrollment));
        doThrow(new RuntimeException("course-service unavailable"))
                .when(courseServiceClient).increaseEnrollmentCount(10L);

        assertThrows(RuntimeException.class,
                () -> enrollmentService.activateEnrollment(3L, 10L));

        assertEquals(Enrollment.Status.PENDING, enrollment.getStatus());
        verifyNoInteractions(kafkaProducer);
    }
}
