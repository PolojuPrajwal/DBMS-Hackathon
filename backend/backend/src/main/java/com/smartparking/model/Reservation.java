package com.smartparking.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Data
public class Reservation {
    public enum Status { ACTIVE, CANCELLED, COMPLETED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Slot slot;
    @ManyToOne(optional = false) private Vehicle vehicle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Enumerated(EnumType.STRING) private Status status = Status.ACTIVE;
}
