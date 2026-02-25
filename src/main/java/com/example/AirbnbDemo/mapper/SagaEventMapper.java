package com.example.AirbnbDemo.mapper;

import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaStatus;

import java.time.LocalDateTime;
import java.util.Map;

public class SagaEventMapper {

    public static SagaEvent toEntity(String id,String eventType,String step,
                                     Map<String,Object> payload
    ){
        return SagaEvent.builder()
                .sagaId(id)
                .eventType(eventType)
                .step(step)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .status(SagaStatus.PENDING)
                .build();
    }
}
