package com.lecture.course.service;

import com.lecture.course.entity.Course;
import com.lecture.course.exception.NotFoundException;
import com.lecture.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseServiceDetailVisibilityTest {

    private static final Long SUPPLIER_ID = 6L;
    private static final Long BUYER_ID = 8L;

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserServiceClient userServiceClient = mock(UserServiceClient.class);
    private final CourseService courseService = new CourseService(courseRepository, userServiceClient);

    private Course lotWith(Course.Status status) {
        Course course = Course.builder()
                .id(1L)
                .title("폐배터리 블랙매스 5톤")
                .category(Course.Category.BATTERY)
                .instructorId(SUPPLIER_ID)
                .status(status)
                .build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        return course;
    }

    /** 구매 완료 로트를 구매기업이 상세 조회할 수 있어야 한다 (issue #4) */
    @Test
    void soldLotIsVisibleToBuyer() {
        lotWith(Course.Status.SOLD);

        assertEquals(Course.Status.SOLD, courseService.getCourse(1L, BUYER_ID).getStatus());
    }

    @Test
    void approvedAndSoldLotsAreVisibleAnonymously() {
        lotWith(Course.Status.APPROVED);
        assertEquals(Course.Status.APPROVED, courseService.getCourse(1L, null).getStatus());

        lotWith(Course.Status.SOLD);
        assertEquals(Course.Status.SOLD, courseService.getCourse(1L, null).getStatus());
    }

    /** 한 번도 공개된 적 없는 로트는 존재 자체를 숨긴다 */
    @ParameterizedTest
    @EnumSource(value = Course.Status.class, names = {"PENDING", "REJECTED", "WITHDRAWN"})
    void neverPublishedLotsStayHiddenFromOthers(Course.Status status) {
        lotWith(status);

        assertThrows(NotFoundException.class, () -> courseService.getCourse(1L, BUYER_ID));
        assertThrows(NotFoundException.class, () -> courseService.getCourse(1L, null));
    }

    /** 공급기업 본인은 상태와 무관하게 자기 로트를 볼 수 있다 */
    @ParameterizedTest
    @EnumSource(Course.Status.class)
    void supplierAlwaysSeesOwnLot(Course.Status status) {
        lotWith(status);

        assertEquals(status, courseService.getCourse(1L, SUPPLIER_ID).getStatus());
    }
}
