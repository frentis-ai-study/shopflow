package com.shopflow.account.domain;

import com.shopflow.account.repository.UserRepository;
import com.shopflow.common.error.DomainException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.regex.Pattern;

/** 가입·역할 부여 등 계정 도메인 로직(FR-001~004). */
@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** 회원가입. 기본 역할은 BUYER. 이메일 형식·중복·비밀번호 정책 검증(FR-002). */
    @Transactional
    public User signup(String email, String rawPassword, String displayName) {
        validateEmail(email);
        validatePassword(rawPassword);
        if (displayName == null || displayName.isBlank()) {
            throw DomainException.badRequest("표시명은 필수입니다");
        }
        if (users.existsByEmail(email)) {
            throw DomainException.conflict("이미 사용 중인 이메일입니다");
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), displayName,
                EnumSet.of(Role.BUYER), Instant.now(clock));
        return users.save(user);
    }

    /** 판매자 역할 부여. */
    @Transactional
    public User grantSellerRole(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> DomainException.notFound("사용자를 찾을 수 없습니다"));
        user.grantRole(Role.SELLER);
        return user;
    }

    static void validateEmail(String email) {
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw DomainException.badRequest("이메일 형식이 올바르지 않습니다");
        }
    }

    static void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw DomainException.badRequest("비밀번호는 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다");
        }
    }
}
