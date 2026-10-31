package com.smartparking.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String plate;
    private String ownerName;
    private String phone;
}
