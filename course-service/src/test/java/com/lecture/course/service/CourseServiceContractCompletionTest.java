package com.lecture.course.service;

import com.lecture.course.entity.Course;
import com.lecture.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseServiceContractCompletionTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserServiceClient userServiceClient = mock(UserServiceClient.class);
    private final CourseService courseService = new CourseService(courseRepository, userServiceClient);

    @Test
    void approvedLotIsSoldExactlyOnce() {
        Course course = Course.builder()
                .id(10L)
                .status(Course.Status.APPROVED)
                .enrollmentCount(0)
                .build();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        courseService.increaseEnrollmentCount(10L);
        courseService.increaseEnrollmentCount(10L);

        assertEquals(Course.Status.SOLD, course.getStatus());
        assertEquals(1, course.getEnrollmentCount());
    }

    @Test
    void nonApprovedLotCannotBeCompleted() {
        Course course = Course.builder()
                .id(10L)
                .status(Course.Status.PENDING)
                .enrollmentCount(0)
                .build();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        assertThrows(IllegalStateException.class,
                () -> courseService.increaseEnrollmentCount(10L));
        assertEquals(0, course.getEnrollmentCount());
        assertEquals(Course.Status.PENDING, course.getStatus());
    }
}
