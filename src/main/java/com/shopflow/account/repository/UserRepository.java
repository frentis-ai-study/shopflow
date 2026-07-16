package com.shopflow.account.repository;

import com.shopflow.account.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

/**
 * 사용자 리포지토리. 인증·가입에 쓰이며 REST로 노출하지 않는다(passwordHash 유출 방지, 검토
 * 보고서 #3). detection-strategy=annotated + 아래 exported=false로 자동 노출을 막는다.
 */
@RestResource(exported = false)
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
