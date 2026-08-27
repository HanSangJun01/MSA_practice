package com.lecture.course.exception;

/**
 * 404 - 없는 로트, 또는 구매기업에게 노출하면 안 되는 비승인 로트
 * (존재 자체를 숨겨야 하므로 403 이 아니라 404 로 응답한다)
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
