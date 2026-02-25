package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.dtos.BookingDTO;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
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
    public ResponseEntity<BookingReadModel> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }


    @GetMapping
    public ResponseEntity<List<BookingReadModel>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
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
