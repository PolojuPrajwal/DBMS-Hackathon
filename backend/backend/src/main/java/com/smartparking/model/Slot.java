package com.smartparking.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"location_id", "code"}))
public class Slot {
    public enum Status { AVAILABLE, OCCUPIED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Location location;
    @Column(nullable = false) private String code;
    private String type = "CAR";
    @Enumerated(EnumType.STRING) private Status status = Status.AVAILABLE;
}
