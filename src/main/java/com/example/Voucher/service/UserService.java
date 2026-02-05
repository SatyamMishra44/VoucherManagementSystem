package com.example.Voucher.service;

import com.example.Voucher.entity.User;

import java.util.List;
import java.util.Optional;


// This service acts as the business-layer abstraction
// for all the user-related operations
public interface UserService {

    //Fetch a user by their Unique ID
    Optional<User> findById(Long userId);

    User createUser(User user);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> getAllUsers();

    boolean existsById(Long userId);

    Optional<User> findByEmail(String email);
}
