package com.smartparking.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Data
public class ParkingRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Vehicle vehicle;
    @ManyToOne(optional = false) private Slot slot;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Double fee;
}
