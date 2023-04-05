package com.example.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4JConfig {
    // CircuitBreaker, TimeLimiter 구성 커스터마이징
    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> globalCustomConfiguration() {
        // CircuitBreaker가 구성 설정
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                // 호출 실패율
                .failureRateThreshold(4)
                // CircuitBreaker를 open한 상태를 유지하는 지속기간
                // 이 기간 이후에 half-open 상태
                // default: 60seconds
                .waitDurationInOpenState(Duration.ofMillis(1000))
                // CircuitBreaker가 닫힐 때 통화 결과를 기록하는 데 사용되는 슬라이딩 창의 유형을 구성
                // 카운트 기반 또는 시간 기반
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                // CirbuitBreaker가 닫힐 때 호출 결과를 기록하는데 사용되는 슬라이딩 창의 크기를 구성
                // default: 100
                .slidingWindowType(2)
                .build();

        // TimeLimiter 구성설정
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                // 호출 시간 제한 설정
                .timeoutDuration(Duration.ofSeconds(4))
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(timeLimiterConfig)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build());
    }
}
