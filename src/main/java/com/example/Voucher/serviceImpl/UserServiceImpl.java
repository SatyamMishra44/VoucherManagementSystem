package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.User;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.service.UserService;

import java.util.Optional;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    //implemented the constructor injection to inject the bean
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    //internally
    // if user exist-> optional.of(user)
    // if not exist -> optional.empty()
    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }


    //check if user exists
    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);

    }
}
