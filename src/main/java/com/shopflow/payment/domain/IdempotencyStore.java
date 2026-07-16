package com.shopflow.payment.domain;

import com.shopflow.payment.repository.PaymentIdempotencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * 멱등 키 시작/조회(검토 보고서 #3). {@link #begin}은 독립 트랜잭션(REQUIRES_NEW)으로 STARTED를
 * 즉시 커밋해 경합 창을 줄인다. 동일 키 재요청은 기존 결과(DONE의 주문)를 반환한다.
 */
@Service
public class IdempotencyStore {

    public enum Outcome { STARTED, ALREADY_DONE, IN_PROGRESS, PREVIOUSLY_FAILED }

    public record BeginResult(Outcome outcome, Long orderId) {
    }

    private final PaymentIdempotencyRepository repo;
    private final Clock clock;

    public IdempotencyStore(PaymentIdempotencyRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * 멱등 시작. 이미 있으면 상태로 분류해 반환, 없으면 STARTED를 삽입한다.
     * 동시 삽입 경합에서 지면 {@link DataIntegrityViolationException}이 전파되며(이 독립
     * 트랜잭션은 롤백), 호출자는 이를 "진행 중"으로 처리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BeginResult begin(String key, String requestHash) {
        Optional<PaymentIdempotency> existing = repo.findById(key);
        if (existing.isPresent()) {
            return classify(existing.get());
        }
        repo.saveAndFlush(new PaymentIdempotency(key, requestHash, Instant.now(clock)));
        return new BeginResult(Outcome.STARTED, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        repo.findById(key).ifPresent(PaymentIdempotency::markFailed);
    }

    private BeginResult classify(PaymentIdempotency rec) {
        return switch (rec.getStatus()) {
            case DONE -> new BeginResult(Outcome.ALREADY_DONE, rec.getOrderId());
            case STARTED -> new BeginResult(Outcome.IN_PROGRESS, null);
            case FAILED -> new BeginResult(Outcome.PREVIOUSLY_FAILED, null);
        };
    }
}
