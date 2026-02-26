package com.example.AirbnbDemo.saga;


import com.example.AirbnbDemo.dlq.DeadLetterEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryableSagaProcessor {

    @Value("${saga.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${saga.retry.delay-ms:1000}")
    private long retryDelayMs;

    private final SagaEventProcessor sagaEventProcessor;
    private final DeadLetterEventPublisher deadLetterEventPublisher;

    public void processWithRetry(SagaEvent sagaEvent) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                sagaEventProcessor.processEvent(sagaEvent);
                return;//success - exit immediately
            } catch (Exception e) {
                lastException = e;
                log.warn(
                        "Saga event processing failed on attempt {}/{} for sagaId={} type={}: {}",
                        attempt, maxAttempts,
                        sagaEvent.getSagaId(),
                        sagaEvent.getEventType(),
                        e.getMessage()
                );
                if (attempt < maxAttempts) {
                    sleepWithBackOff(attempt);
                }
            }
        }
        log.error( "All {} attempts failed for sagaId={} type={}. Moving to DLQ.",
                maxAttempts, sagaEvent.getSagaId(), sagaEvent.getEventType());
        deadLetterEventPublisher.publish(sagaEvent, lastException, maxAttempts);
    }

    private void sleepWithBackOff(int attempt){
        try{
            long delay = retryDelayMs * (1L << (attempt - 1));
            log.info("Retrying in {} ms....", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted");
        }
    }


}
