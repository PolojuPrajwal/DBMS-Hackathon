package com.smartparking.repo;
import com.smartparking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    /** Active reservations on a slot overlapping the window [start, end). */
    List<Reservation> findBySlotIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long slotId, Reservation.Status status, LocalDateTime end, LocalDateTime start);
    long countByStatus(Reservation.Status status);
    List<Reservation> findByVehiclePlateOrderByStartTimeDesc(String plate);
}
