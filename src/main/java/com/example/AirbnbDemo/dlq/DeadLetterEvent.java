package com.example.AirbnbDemo.dlq;


import com.example.AirbnbDemo.saga.SagaEvent;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeadLetterEvent {
    private SagaEvent originalEvent;
    private String errorMessage;
    private int attemptCount;
    private LocalDateTime failedAt;
}