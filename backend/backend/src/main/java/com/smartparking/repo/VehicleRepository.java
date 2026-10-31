package com.smartparking.repo;
import com.smartparking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByPlate(String plate);
}
