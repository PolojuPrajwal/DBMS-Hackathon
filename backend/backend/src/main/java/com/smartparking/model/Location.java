package com.smartparking.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String address;
    private double ratePerHour = 20;
}
