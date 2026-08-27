package com.lecture.user.dto;

import com.lecture.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    // 회원가입 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        private String password;

        @Schema(description = "기업명. 별도 companyName 필드를 두지 않고 이 값을 기업명으로 쓴다",
                example = "SK순환자원")
        @NotBlank(message = "기업명은 필수입니다")
        private String name;

        @Schema(description = "인증 서버 호환 역할. SUPPLIER/BUYER/INTERMEDIARY를 여기 넣지 않는다",
                example = "INSTRUCTOR")
        private User.Role role; // STUDENT or INSTRUCTOR

        @Schema(description = """
                비즈니스 기업 유형. 생략하면 role에서 유추한다 (STUDENT → BUYER, INSTRUCTOR → SUPPLIER).
                허용 조합은 STUDENT+BUYER, INSTRUCTOR+SUPPLIER, INSTRUCTOR+INTERMEDIARY 뿐이다.
                """, example = "SUPPLIER")
        private User.CompanyType companyType;
    }

    // 사용자 정보 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private User.Role role;

        @Schema(description = "기업 유형. JWT 클레임에는 없으므로 이 API로만 확인할 수 있다")
        private User.CompanyType companyType;

        private LocalDateTime createdAt;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .companyType(user.getCompanyType())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    // 공통 API 응답 래퍼
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
