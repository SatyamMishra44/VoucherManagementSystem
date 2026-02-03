package com.example.Voucher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @NotBlank
    @Column(nullable = false)
    private String password;   // should store HASH, not plain text

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be exactly 10 digits"
    )
    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Email contains invalid characters")
    @Column(nullable = false, unique = true)
    private String email;


    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ✅ Required by JPA
    protected User() {
    }

    // Optional constructor for manual creation
    public User(String firstName, String lastName, String password,
                String phoneNumber, String email,LocalDateTime createdAt) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.createdAt = createdAt;
    }


    // Getters
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPassword() { return password; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
