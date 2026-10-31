package com.smartparking.repo;
import com.smartparking.model.ParkingRecord;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.Optional;
public interface ParkingRecordRepository extends JpaRepository<ParkingRecord, Long> {
    Optional<ParkingRecord> findFirstByVehiclePlateAndExitTimeIsNull(String plate);
    long countByEntryTimeAfter(LocalDateTime t);
    @Query("select coalesce(sum(r.fee),0) from ParkingRecord r")
    double totalRevenue();
}
