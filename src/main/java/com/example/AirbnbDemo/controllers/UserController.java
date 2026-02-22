package com.example.AirbnbDemo.controllers;


import com.example.AirbnbDemo.Mapper.UserMapper;
import com.example.AirbnbDemo.dtos.CreateUserDTO;
import com.example.AirbnbDemo.dtos.UserDTO;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.services.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PostMapping
    public ResponseEntity<UserDTO> CreateUser(@Valid @RequestBody CreateUserDTO dto) {
        User user=userService.createUser(dto);
        UserDTO userDTO= UserMapper.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("/email")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> userDTOList=userService.getAllUsers().stream().map(UserMapper::toDTO).toList();
        return ResponseEntity.ok(userDTOList);
    }



    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,@Valid @RequestBody CreateUserDTO request) {
        return ResponseEntity.ok(UserMapper.toDTO(userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
