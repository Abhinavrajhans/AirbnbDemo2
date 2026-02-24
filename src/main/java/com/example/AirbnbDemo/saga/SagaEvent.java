package com.example.AirbnbDemo.saga;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SagaEvent implements Serializable {
    private String sagaId;
    private String eventType;
    private String step;
    private Map<String,Object> payload;
    private LocalDateTime timestamp;
    private SagaStatus status;

    public String toString(){
        return eventType+" "+payload.get("bookingId")+" "+status;
    }
}
