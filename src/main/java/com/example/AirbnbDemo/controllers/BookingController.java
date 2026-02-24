package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.dtos.BookingDTO;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.services.IBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
public class BookingController {

    private final IBookingService bookingService;


    @PostMapping
    public ResponseEntity<BookingDTO> createBooking(@Valid @RequestBody CreateBookingDTO dto) {
        Booking booking = bookingService.createBooking(dto);
        BookingDTO bookingDTO = BookingMapper.toDTO(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingDTO);
    }

    @PutMapping
    public ResponseEntity<String> updateBooking(@Valid @RequestBody UpdateBookingRequest dto) {
        String message= bookingService.updateBooking(dto);
        return ResponseEntity.ok(message);
    }


    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(BookingMapper.toDTO(booking));
    }


    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        List<BookingDTO> bookingDTOList=bookingService.getAllBookings().stream().map(BookingMapper::toDTO).toList();
        return ResponseEntity.ok(bookingDTOList);
    }

    @GetMapping("/user")
    public ResponseEntity<List<BookingDTO>> getUserHistory(@RequestParam Long userId) {
        List<BookingDTO> bookingDTOList=bookingService.getUserBookingHistory(userId)
                .stream().map(BookingMapper::toDTO).toList();
        return ResponseEntity.ok(bookingDTOList);
    }
    @GetMapping("/airbnb")
    public ResponseEntity<List<BookingDTO>> getAirbnbHistory(@RequestParam Long airbnbId) {
        List<BookingDTO> bookingDTOList=bookingService.getAirbnbBookingHistory(airbnbId)
                .stream().map(BookingMapper::toDTO).toList();
        return ResponseEntity.ok(bookingDTOList);
    }




}
