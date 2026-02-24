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
    public Optional<Booking> findBookingByIdempotencyKey(String idempotencyKey) {
        BookingReadModel bookingReadModel = redisReadRepository.findBookingByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ResourceNotFoundException("BookingReadModel with Idempotency key:"+idempotencyKey+" not found"));

        if(bookingReadModel!=null){
            User user= userRepository.findById(bookingReadModel.getUserId())
                    .orElseThrow(()->new ResourceNotFoundException("User with ID:"+bookingReadModel.getUserId()+" not found"));
            Airbnb airbnb = airbnbRepository.findById(bookingReadModel.getAirbnbId())
                    .orElseThrow(()->new ResourceNotFoundException("Airbnb with ID:"+bookingReadModel.getAirbnbId()+" not found"));

            Booking booking = BookingMapper.ToEntityFromReadModel(bookingReadModel,user,airbnb);
            return Optional.of(booking);
        }
        return bookingRepository.findByIdempotencyKey(idempotencyKey);
    }
}
