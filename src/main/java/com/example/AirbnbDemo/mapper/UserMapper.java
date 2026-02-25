package com.example.AirbnbDemo.mapper;

import com.example.AirbnbDemo.dtos.CreateUserDTO;
import com.example.AirbnbDemo.dtos.UserDTO;
import com.example.AirbnbDemo.models.User;

public class UserMapper {

    public static User toEntity(CreateUserDTO dto){
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .build();
    }

    public static UserDTO toDTO(User user){
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
