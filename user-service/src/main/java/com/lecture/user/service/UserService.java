package com.lecture.user.service;

import com.lecture.user.dto.UserDto;
import com.lecture.user.entity.User;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 기업 회원가입
     * - role 은 인증 서버 호환을 위해 STUDENT | INSTRUCTOR 를 유지한다
     * - 기업 구분은 companyType 으로 저장한다
     */
    @Transactional
    public UserDto.UserResponse register(UserDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + request.getEmail());
        }

        User.Role role = request.getRole() != null ? request.getRole() : User.Role.STUDENT;
        User.CompanyType companyType = resolveCompanyType(role, request.getCompanyType());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(role)
                .companyType(companyType)
                .build();

        User savedUser = userRepository.save(user);
        return UserDto.UserResponse.from(savedUser);
    }

    /**
     * role 과 companyType 조합 검증
     * - STUDENT      + BUYER                    = 구매기업
     * - INSTRUCTOR   + SUPPLIER | INTERMEDIARY  = 공급기업 | 중간 승인기업
     * - companyType 생략 시 role 로부터 기본값을 유추한다
     */
    private User.CompanyType resolveCompanyType(User.Role role, User.CompanyType requested) {
        if (requested == null) {
            return role == User.Role.INSTRUCTOR
                    ? User.CompanyType.SUPPLIER
                    : User.CompanyType.BUYER;
        }

        boolean valid = switch (requested) {
            case BUYER -> role == User.Role.STUDENT;
            case SUPPLIER, INTERMEDIARY -> role == User.Role.INSTRUCTOR;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "role 과 companyType 조합이 올바르지 않습니다: role=" + role + ", companyType=" + requested
                            + " (STUDENT+BUYER, INSTRUCTOR+SUPPLIER, INSTRUCTOR+INTERMEDIARY 만 허용)");
        }
        return requested;
    }

    /**
     * 사용자 단건 조회
     */
    public UserDto.UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));
        return UserDto.UserResponse.from(user);
    }

    /**
     * 이메일로 사용자 조회 (서비스 간 내부 호출용)
     */
    public UserDto.UserResponse getUserByEmail(String email) {
        System.out.println(">>> getUserByEmail email = " + email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        return UserDto.UserResponse.from(user);
    }
}
