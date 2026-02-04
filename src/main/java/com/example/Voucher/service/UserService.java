package com.example.Voucher.service;

import com.example.Voucher.entity.User;

import java.util.Optional;


// This service acts as the business-layer abstraction
// for all the user-related operations
public interface UserService {

    //Fetch a user by their Unique ID
    Optional<User> findById(Long userId);


    boolean existsById(Long userId);

    Optional<User> findByEmail(String email);
}
