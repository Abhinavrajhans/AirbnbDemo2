package com.example.AirbnbDemo.services;

import java.util.List;
import com.example.AirbnbDemo.dtos.CreateUserDTO;
import com.example.AirbnbDemo.models.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;


public interface IUserService {
    User createUser(CreateUserDTO dto);
    User getUserById(Long id);
    User getUserByEmail(String email);
    List<User> getAllUsers();
    User updateUser(Long id,CreateUserDTO dto);
    void deleteUser(Long id);
}
