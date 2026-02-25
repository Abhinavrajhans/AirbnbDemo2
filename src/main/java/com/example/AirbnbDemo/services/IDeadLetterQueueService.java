package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dlq.DeadLetterEvent;

import java.util.List;

public interface IDeadLetterQueueService {
    Long getDlqSize();
    List<DeadLetterEvent> listEvents();
    String replayOne();
    String replayAll();
    String clearDlq();
}
