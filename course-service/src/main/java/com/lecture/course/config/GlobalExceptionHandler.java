package com.lecture.course.config;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.exception.ForbiddenException;
import com.lecture.course.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 에러 응답은 기존 형식 { success, message, data } 를 그대로 유지한다.
 *
 * 상황별 코드
 *   허용되지 않은 상태 전이            400
 *   필수 헤더 X-User-Id 누락           400
 *   enum 밖 카테고리·성분명            400
 *   본인 로트가 아닌데 수정·철회       403
 *   중간기업이 아닌데 승인·거절·보정   403
 *   구매기업이 비승인 로트를 상세 조회 404 (존재 자체를 숨긴다)
 *   없는 로트                          404
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CourseDto.ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CourseDto.ApiResponse.error(e.getMessage()));
    }

    /** 상태 전이표 밖의 전이 */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error(e.getMessage()));
    }

    /** 필수 헤더 X-User-Id 누락 - 기본값을 지어내지 않고 400 으로 거절한다 */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error("필수 헤더가 누락되었습니다: " + e.getHeaderName()));
    }

    /** 본문의 enum 밖 값 (성분명 "리튬" 등) 및 파싱 불가 요청 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error("요청 본문을 해석할 수 없습니다. 카테고리·성분명이 허용된 값인지 확인하세요"));
    }

    /** Path Variable 의 enum 밖 카테고리 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(CourseDto.ApiResponse.error("잘못된 요청 값입니다: " + e.getName()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CourseDto.ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CourseDto.ApiResponse<Void>> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CourseDto.ApiResponse.error("서버 오류가 발생했습니다"));
    }
}
