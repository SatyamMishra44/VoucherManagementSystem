package com.example.Voucher.serviceImpl;

import com.example.Voucher.entity.User;
import com.example.Voucher.repository.UserRepository;
import com.example.Voucher.service.UserService;
import com.example.Voucher.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //implemented the constructor injection to inject the bean
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    //internally
    // if user exist-> optional.of(user)
    // if not exist -> optional.empty()
    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findByIdAndTenantId(userId, TenantContext.requireTenantId());
    }

    @Override
    public User createUser(User user) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setTenantId(TenantContext.requireTenantId());
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndTenantId(email, TenantContext.requireTenantId());
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumberAndTenantId(phoneNumber, TenantContext.requireTenantId());
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByTenantId(TenantContext.requireTenantId());
    }

    @Override
    public List<User> getUsersWithFilters(String firstName, String lastName, String email, String phoneNumber, Boolean enabled) {
        String normalizedFirstName = trimToNull(firstName);
        String normalizedLastName = trimToNull(lastName);
        String normalizedEmail = trimToNull(email);
        String normalizedPhoneNumber = trimToNull(phoneNumber);


        if (normalizedFirstName == null && normalizedLastName == null && normalizedEmail == null && normalizedPhoneNumber == null && enabled == null) {
            return getAllUsers();
        }

        return userRepository.findAllByTenantIdWithFilters(
                TenantContext.requireTenantId(),
                normalizedFirstName,
                normalizedLastName,
                normalizedEmail,
                normalizedPhoneNumber,
                enabled
        );
    }




    //check if user exists
    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsByIdAndTenantId(userId, TenantContext.requireTenantId());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndTenantId(email, TenantContext.requireTenantId());

    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
