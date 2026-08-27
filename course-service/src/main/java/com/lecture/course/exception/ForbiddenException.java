package com.lecture.course.exception;

/**
 * 403 - 본인 로트가 아닌데 수정·철회를 시도하거나,
 *       중간기업이 아닌데 승인·거절·설명 보정을 시도한 경우
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
