package com.lecture.user.controller;

import com.lecture.user.dto.UserDto;
import com.lecture.user.service.UserService;
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

@Tag(name = "기업", description = """
        순환거래 참여 기업 관리.

        인증 서버가 프리빌트 이미지라 `role`은 `STUDENT | INSTRUCTOR`를 유지하고,
        기업 구분은 `companyType`으로 분리한다. 기업명은 별도 필드 없이 `name`을 쓴다.

        | role | companyType | 의미 |
        |---|---|---|
        | STUDENT | BUYER | 구매기업 |
        | INSTRUCTOR | SUPPLIER | 공급기업 |
        | INSTRUCTOR | INTERMEDIARY | 중간 승인기업 |
        """)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /users/register - 회원가입 (인증 불필요)
     */
    @Operation(summary = "기업 회원가입", description = """
            순환거래 참여 기업을 등록한다. 인증이 필요 없다.

            `companyType`을 생략하면 `role`에서 유추한다 (STUDENT → BUYER, INSTRUCTOR → SUPPLIER).
            조합이 맞지 않으면 400으로 거절한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400",
                    description = "이메일 중복, 비밀번호 8자 미만, 또는 role-companyType 조합 불일치")
    })
    @PostMapping("/register")
    public ResponseEntity<UserDto.ApiResponse<UserDto.UserResponse>> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        UserDto.UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserDto.ApiResponse.success(response));
    }

    /**
     * GET /users/{id} - 사용자 조회 (인증 필요)
     */
    @Operation(summary = "기업 정보 조회", description = "기업 ID로 단건 조회한다. `name`이 기업명이다.")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto.ApiResponse<UserDto.UserResponse>> getUser(
            @Parameter(description = "기업 ID", example = "2") @PathVariable Long id) {
        UserDto.UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(UserDto.ApiResponse.success(response));
    }

    /**
     * GET /users/me - 내 정보 조회
     * API Gateway가 전달한 X-User-Id 헤더(숫자 userId)를 사용
     */
    @Operation(summary = "내 기업 정보 조회", description = """
            토큰 주체의 기업 정보를 반환한다.

            `companyType`은 JWT 클레임에 없으므로, 다른 서비스가 중간기업 여부를 확인하려면
            이 엔드포인트의 내부용 버전(`/api/users/internal/{id}`)을 호출해야 한다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "`X-User-Id` 헤더 누락 (Gateway를 거치지 않은 직접 호출)")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto.ApiResponse<UserDto.UserResponse>> getMe(
            @Parameter(description = "Gateway의 JwtAuthenticationFilter가 주입하는 기업 ID. 직접 호출 시에는 없다",
                    required = true, example = "3")
            @RequestHeader("X-User-Id") Long userId) {

        UserDto.UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(UserDto.ApiResponse.success(response));
    }

    /**
     * GET /users/internal/{id} - 서비스 간 내부 호출용 (Client Credentials)
     */
    @Operation(summary = "[내부] 기업 정보 조회", description = """
            서비스 간 호출용. `{ success, message, data }` 래퍼 없이 본문을 바로 반환한다.

            course-service가 승인·설명 보정 인가를 판정할 때 이 API로 `companyType`을 읽는다.
            JWT에 `companyType` 클레임이 없어 이 경로가 유일한 확인 수단이다.
            """)
    @GetMapping("/internal/{id}")
    public ResponseEntity<UserDto.UserResponse> getUserInternal(
            @Parameter(description = "기업 ID", example = "7") @PathVariable Long id) {
        UserDto.UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}