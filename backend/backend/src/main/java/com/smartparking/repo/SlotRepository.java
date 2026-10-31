package com.smartparking.repo;
import com.smartparking.model.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByLocationId(Long locationId);
    long countByStatus(Slot.Status status);
}
