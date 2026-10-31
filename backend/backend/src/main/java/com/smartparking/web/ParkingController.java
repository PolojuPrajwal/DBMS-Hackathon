package com.smartparking.web;

import com.smartparking.model.*;
import com.smartparking.repo.*;
import com.smartparking.service.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ParkingController {
    private final ParkingService svc;
    private final LocationRepository locations;
    private final SlotRepository slots;
    private final ReservationRepository reservations;
    private final ParkingRecordRepository records;

    public record ReserveReq(String plate, Long slotId, LocalDateTime start, LocalDateTime end) {}
    public record EntryReq(String plate, Long slotId) {}

    @GetMapping("/locations") List<Location> locations() { return locations.findAll(); }
    @PostMapping("/locations") Location addLocation(@RequestBody Location l) { return locations.save(l); }
    @GetMapping("/locations/{id}/slots") List<Slot> slots(@PathVariable Long id) { return slots.findByLocationId(id); }
    @PostMapping("/slots") Slot addSlot(@RequestBody Slot s) { return slots.save(s); }

    @GetMapping("/locations/{id}/available")
    List<Slot> available(@PathVariable Long id,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return svc.availableSlots(id, from, to);
    }

    @PostMapping("/vehicles") Vehicle register(@RequestBody Vehicle v) { return svc.register(v); }

    @PostMapping("/reservations") Reservation reserve(@RequestBody ReserveReq r) { return svc.reserve(r.plate(), r.slotId(), r.start(), r.end()); }
    @GetMapping("/reservations") List<Reservation> reservations(@RequestParam(required = false) String plate) {
        return plate == null ? reservations.findAll() : reservations.findByVehiclePlateOrderByStartTimeDesc(plate.toUpperCase());
    }
    @DeleteMapping("/reservations/{id}") Reservation cancel(@PathVariable Long id) { return svc.cancel(id); }

    @PostMapping("/entry") ParkingRecord entry(@RequestBody EntryReq r) { return svc.entry(r.plate(), r.slotId()); }
    @PostMapping("/exit/{plate}") ParkingRecord exit(@PathVariable String plate) { return svc.exit(plate); }
    @GetMapping("/records") List<ParkingRecord> records() { return records.findAll(); }

    @GetMapping("/admin/dashboard") Map<String, Object> dashboard() { return svc.dashboard(); }
    @GetMapping("/admin/predict") Object predict() { return svc.predict(); }
}
