package com.smartparking.repo;
import com.smartparking.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LocationRepository extends JpaRepository<Location, Long> {}
