package com.example.hellocircuitbreakerresilience4j;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.circuitbreaker.commons.Customizer;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.core.style.ToStringCreator;

import java.time.Duration;

public class CircuitBreakerFactoryCustomizer implements Customizer<Resilience4JCircuitBreakerFactory> {

    private final Logger log = LoggerFactory.getLogger(CircuitBreakerFactoryCustomizer.class);

    @Override
    public void customize(Resilience4JCircuitBreakerFactory factory) {
        factory
            .configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig
                    .custom()
                    // https://github.com/Netflix/Hystrix/wiki/How-it-Works#circuit-breaker
                    .failureRateThreshold(50) // 50%
                    .ringBufferSizeInClosedState(30) // 30 * 0.5 => 15/
                    .ringBufferSizeInHalfOpenState(20) // 20 * 0.5 => 10
                    .waitDurationInOpenState(Duration.ofSeconds(3) /* for demo */)
                    .build())
                .timeLimiterConfig(TimeLimiterConfig
                    .custom()
                    .build())
                .build());
        factory.addCircuitBreakerCustomizer(circuitBreaker -> {
            // this.monitoringLog(circuitBreaker);
            final CircuitBreaker.EventPublisher eventPublisher = circuitBreaker.getEventPublisher();
            if (!((EventProcessor) eventPublisher).hasConsumers()) {
                eventPublisher.onStateTransition(event -> log.info("{}: {}", event.getCircuitBreakerName(), event.getStateTransition()));
            }
        }, "hello");
    }

    void monitoringLog(CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        log.info("{}", new ToStringCreator(metrics)
            .append("failureRate", metrics.getFailureRate())
            .append("bufferedCalls", metrics.getNumberOfBufferedCalls())
            .append("failedCalls", metrics.getNumberOfFailedCalls())
            .append("notPermittedCalls", metrics.getNumberOfNotPermittedCalls())
            .append("successfulCalls", metrics.getNumberOfSuccessfulCalls())
            .append("maxNumberOfBufferedCalls", metrics.getMaxNumberOfBufferedCalls()));
    }
}
