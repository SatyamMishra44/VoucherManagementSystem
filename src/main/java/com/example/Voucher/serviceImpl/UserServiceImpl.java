package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.User;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
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

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
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
