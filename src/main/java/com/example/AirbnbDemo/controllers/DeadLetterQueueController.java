package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.dlq.DeadLetterEvent;
import com.example.AirbnbDemo.services.IDeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueController {

    private final IDeadLetterQueueService deadLetterQueueService;

    // See how many failed events are sitting in the DLQ
    @GetMapping("/size")
    public ResponseEntity<Long> getDlqSize() {
        return ResponseEntity.ok(deadLetterQueueService.getDlqSize());
    }

    // Peek at all DLQ events without removing them
    @GetMapping("/events")
    public ResponseEntity<List<DeadLetterEvent>> getDeadLetterEvents() {
        return ResponseEntity.ok(deadLetterQueueService.listEvents());
    }

    //Replay ONE event from the front of the DLQ
    @PostMapping("/replay/one")
    public ResponseEntity<String> replayOne() {
        return  ResponseEntity.ok(deadLetterQueueService.replayOne());
    }


    // Replay ALL events in the DLQ
    @PostMapping("/replay/all")
    public ResponseEntity<String> replayAll() {
        return  ResponseEntity.ok(deadLetterQueueService.replayAll());
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearDlq() {
        return ResponseEntity.ok(deadLetterQueueService.clearDlq());
    }

}
