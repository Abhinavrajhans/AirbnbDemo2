package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.UserMapper;
import com.example.AirbnbDemo.dtos.CreateUserDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.exceptions.UserEmailAlreadyExistsException;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.repository.writes.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(CreateUserDTO dto){
        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new UserEmailAlreadyExistsException("Email Already Registered :"+dto.getEmail());
        }
        User user= UserMapper.toEntity(dto);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }



    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    @Override
    public User updateUser(Long id, CreateUserDTO dto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existingUser.setName(dto.getName());
        existingUser.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            existingUser.setPassword(dto.getPassword());
        }
        return userRepository.save(existingUser);
    }



    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }




}
