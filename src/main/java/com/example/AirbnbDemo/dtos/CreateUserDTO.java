package com.example.AirbnbDemo.dtos;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserDTO {

    @NotBlank(message="Name is Required")
    private String name;

    @Email(message = "Email must be valid")
    @NotBlank(message="Email is Required")
    private String email;

    @NotBlank(message="Password is Required")
    @Size(min=8 , message = "Password must be at least 8 characters")
    private String password;

}
