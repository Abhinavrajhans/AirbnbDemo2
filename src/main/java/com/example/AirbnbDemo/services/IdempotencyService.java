package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.repository.writes.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService implements IIdempotencyService{

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AirbnbRepository airbnbRepository;
    private final RedisReadRepository redisReadRepository;

    @Override
    public boolean isIdempotencyKeyUsed(String idempotencyKey) {
        return this.findBookingByIdempotencyKey(idempotencyKey).isPresent();
    }

    @Override
    public Optional<BookingReadModel> findBookingByIdempotencyKey(String idempotencyKey) {
        BookingReadModel bookingReadModel = redisReadRepository.findBookingByIdempotencyKey(idempotencyKey);
        if(bookingReadModel!=null){
            return Optional.of(bookingReadModel);
        }
        Booking booking = bookingRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if(booking!=null) return Optional.of(BookingMapper.toReadModel(booking));
        return Optional.empty();

    }
}
