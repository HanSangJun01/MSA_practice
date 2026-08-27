package com.lecture.course.service;

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
 * companyType 은 JWT 클레임에 없다. Auth Server 가 프리빌트 이미지라
 * 클레임을 추가할 수 없고 Gateway 도 role 만 X-User-Role 로 넘긴다.
 * 따라서 중간기업 여부 확인은 GET /api/users/internal/{id} 조회가 유일한 경로다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    public UserInfo getUser(Long userId) {
        try {
            UserInfo user = webClientBuilder.build()
                    .get()
                    .uri("http://user-service/api/users/internal/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserInfo.class)
                    .block();

            if (user == null) {
                throw new RuntimeException("User Service 응답 본문이 비어 있습니다");
            }
            return user;
        } catch (Exception e) {
            log.error("[UserServiceClient] 기업 정보 조회 실패 - userId: {}, error: {}",
                    userId, e.getMessage());
            throw new RuntimeException("User Service 기업 정보 조회 실패: " + userId);
        }
    }

    /**
     * 공급기업명 조달 - 로트 등록 시 비정규화 저장용
     * user-service 가 잠시 죽어도 로트 등록 자체는 막지 않는다
     */
    public String findCompanyNameOrNull(Long userId) {
        try {
            return getUser(userId).getName();
        } catch (Exception e) {
            log.warn("[UserServiceClient] 공급기업명 조달 실패 - userId: {} (supplierName 은 비워둔다)", userId);
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
        private CompanyType companyType;

        public boolean isIntermediary() {
            return companyType == CompanyType.INTERMEDIARY;
        }
    }

    public enum CompanyType {
        BUYER, SUPPLIER, INTERMEDIARY
    }
}
