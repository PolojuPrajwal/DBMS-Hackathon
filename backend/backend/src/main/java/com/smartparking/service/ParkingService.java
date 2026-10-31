package com.smartparking.service;

import com.smartparking.model.*;
import com.smartparking.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ParkingService {
    private final SlotRepository slots;
    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final ParkingRecordRepository records;
    @Value("${predictor.url}") private String predictorUrl;

    private ResponseStatusException err(HttpStatus s, String m) { return new ResponseStatusException(s, m); }

    private List<Reservation> overlapping(Long slotId, LocalDateTime from, LocalDateTime to) {
        return reservations.findBySlotIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(slotId, Reservation.Status.ACTIVE, to, from);
    }

    /** Fee = started hours x the location's hourly rate (minimum one hour). */
    static double fee(long minutes, double ratePerHour) { return Math.ceil(Math.max(1, minutes) / 60.0) * ratePerHour; }

    public Vehicle register(Vehicle v) {
        v.setPlate(v.getPlate().trim().toUpperCase());
        if (vehicles.findByPlate(v.getPlate()).isPresent()) throw err(HttpStatus.CONFLICT, "Vehicle already registered");
        return vehicles.save(v);
    }

    public List<Slot> availableSlots(Long locationId, LocalDateTime from, LocalDateTime to) {
        return slots.findByLocationId(locationId).stream()
            .filter(s -> s.getStatus() == Slot.Status.AVAILABLE && overlapping(s.getId(), from, to).isEmpty()).toList();
    }

    @Transactional
    public Reservation reserve(String plate, Long slotId, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) throw err(HttpStatus.BAD_REQUEST, "End must be after start");
        Vehicle v = vehicles.findByPlate(plate.toUpperCase()).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Vehicle not registered"));
        Slot s = slots.findById(slotId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!overlapping(slotId, start, end).isEmpty()) throw err(HttpStatus.CONFLICT, "Slot already reserved for that time");
        Reservation r = new Reservation(); r.setSlot(s); r.setVehicle(v); r.setStartTime(start); r.setEndTime(end);
        return reservations.save(r);
    }

    @Transactional
    public Reservation cancel(Long id) {
        Reservation r = reservations.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Reservation not found"));
        r.setStatus(Reservation.Status.CANCELLED);
        return r;
    }

    @Transactional
    public ParkingRecord entry(String plate, Long slotId) {
        Vehicle v = vehicles.findByPlate(plate.toUpperCase()).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Vehicle not registered"));
        if (records.findFirstByVehiclePlateAndExitTimeIsNull(v.getPlate()).isPresent()) throw err(HttpStatus.CONFLICT, "Vehicle is already inside");
        Slot s = slots.findById(slotId).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Slot not found"));
        if (s.getStatus() == Slot.Status.OCCUPIED) throw err(HttpStatus.CONFLICT, "Slot occupied");
        LocalDateTime now = LocalDateTime.now();
        if (overlapping(slotId, now, now.plusMinutes(1)).stream().anyMatch(r -> !r.getVehicle().getId().equals(v.getId())))
            throw err(HttpStatus.CONFLICT, "Slot reserved by another vehicle");
        s.setStatus(Slot.Status.OCCUPIED);
        ParkingRecord rec = new ParkingRecord(); rec.setVehicle(v); rec.setSlot(s); rec.setEntryTime(now);
        return records.save(rec);
    }

    @Transactional
    public ParkingRecord exit(String plate) {
        ParkingRecord rec = records.findFirstByVehiclePlateAndExitTimeIsNull(plate.toUpperCase())
            .orElseThrow(() -> err(HttpStatus.NOT_FOUND, "No open parking record"));
        LocalDateTime now = LocalDateTime.now();
        long minutes = Math.max(1, Duration.between(rec.getEntryTime(), now).toMinutes());
        rec.setExitTime(now); rec.setFee(fee(minutes, rec.getSlot().getLocation().getRatePerHour()));
        rec.getSlot().setStatus(Slot.Status.AVAILABLE);
        return rec;
    }

    public Map<String, Object> dashboard() {
        long total = slots.count(), occ = slots.countByStatus(Slot.Status.OCCUPIED);
        return Map.of("totalSlots", total, "occupied", occ, "available", total - occ,
            "occupancyPercent", total == 0 ? 0 : Math.round(occ * 100.0 / total),
            "activeReservations", reservations.countByStatus(Reservation.Status.ACTIVE),
            "entriesToday", records.countByEntryTimeAfter(LocalDate.now().atStartOfDay()),
            "revenue", records.totalRevenue());
    }

    /** Sends historical entry timestamps to the Python ML service; returns a 24h demand forecast. */
    public Object predict() {
        List<String> entries = records.findAll().stream().map(r -> r.getEntryTime().toString()).toList();
        Map<String, Object> body = Map.of("entries", entries, "total_slots", slots.count());
        try { return new RestTemplate().postForObject(predictorUrl, body, Object.class); }
        catch (Exception e) { throw err(HttpStatus.SERVICE_UNAVAILABLE, "Prediction service unavailable: " + e.getMessage()); }
    }
}
