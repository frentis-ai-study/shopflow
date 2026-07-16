package com.shopflow.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * {@link Clock} 빈 주입 — 재고 선점 TTL·상태 전이 시각 등 시간 의존 로직이 이 Clock을 사용한다.
 * 테스트에서 고정/가변 Clock으로 교체해 만료를 결정적으로 재현한다(검토 보고서 #8).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
