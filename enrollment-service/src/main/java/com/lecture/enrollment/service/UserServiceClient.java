package com.lecture.enrollment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * User Service 내부 조회 클라이언트
 *
 * 계약 목록의 공급기업명을 조달한다.
 * 판매 로트 내부 응답(GET /api/courses/internal/{id})에는 supplierName 이 없으므로
 * instructorId 로 기업명을 따로 조회해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * 기업명 조회
     * user-service 가 잠시 죽어도 계약 목록 조회 자체는 막지 않는다
     */
    public String findCompanyNameOrNull(Long userId) {
        if (userId == null) return null;

        try {
            UserInfo user = webClientBuilder.build()
                    .get()
                    .uri("http://user-service/api/users/internal/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserInfo.class)
                    .block();

            return user != null ? user.getName() : null;
        } catch (Exception e) {
            log.warn("[UserServiceClient] 기업명 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return null;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {
        private Long id;
        private String email;
        private String name;
        private String role;
        private String companyType;
    }
}
