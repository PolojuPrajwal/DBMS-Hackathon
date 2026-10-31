package com.smartparking.model;
import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String username;
    @Column(nullable = false) private String password; // BCrypt hash
    @Column(nullable = false) private String role;     // ADMIN or USER
}
