package com.shopflow.unit.account;

import com.shopflow.account.domain.User;
import com.shopflow.account.domain.UserService;
import com.shopflow.account.repository.UserRepository;
import com.shopflow.common.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 가입 검증(이메일 형식·중복·비밀번호 정책) 단위 테스트 — FR-001/002. */
class EmailPasswordPolicyTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);
    private final UserService service = new UserService(users, encoder, clock);

    @Test
    void 잘못된_이메일_형식은_거부() {
        assertThatThrownBy(() -> service.signup("not-an-email", "password123", "홍길동"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("이메일 형식");
    }

    @Test
    void 짧은_비밀번호는_거부() {
        assertThatThrownBy(() -> service.signup("a@b.com", "short", "홍길동"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("비밀번호");
    }

    @Test
    void 중복_이메일은_거부() {
        when(users.existsByEmail("a@b.com")).thenReturn(true);
        assertThatThrownBy(() -> service.signup("a@b.com", "password123", "홍길동"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("이미 사용 중인");
    }

    @Test
    void 정상_가입은_BUYER_역할로_저장() {
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("HASH");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = service.signup("a@b.com", "password123", "홍길동");

        assertThat(user.getEmail()).isEqualTo("a@b.com");
        assertThat(user.getPasswordHash()).isEqualTo("HASH");
        assertThat(user.hasRole(com.shopflow.account.domain.Role.BUYER)).isTrue();
    }
}
