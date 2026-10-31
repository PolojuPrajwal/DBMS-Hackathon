package com.smartparking.service;

import com.smartparking.model.*;
import com.smartparking.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingServiceTest {
    @Mock SlotRepository slots; @Mock VehicleRepository vehicles;
    @Mock ReservationRepository reservations; @Mock ParkingRecordRepository records;
    @InjectMocks ParkingService svc;

    LocalDateTime t = LocalDateTime.of(2026, 11, 1, 10, 0);

    Vehicle vehicle(long id) { Vehicle v = new Vehicle(); v.setId(id); v.setPlate("KA01"); return v; }
    Slot slot(double rate) {
        Location l = new Location(); l.setRatePerHour(rate);
        Slot s = new Slot(); s.setId(1L); s.setLocation(l); return s;
    }
    void overlapReturns(List<Reservation> list) {
        when(reservations.findBySlotIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(eq(1L), eq(Reservation.Status.ACTIVE), any(), any())).thenReturn(list);
    }
    HttpStatus statusOf(ResponseStatusException e) { return HttpStatus.valueOf(e.getStatusCode().value()); }

    @Test void feeRoundsUpToStartedHours() {
        assertEquals(30, ParkingService.fee(1, 30));
        assertEquals(30, ParkingService.fee(60, 30));
        assertEquals(60, ParkingService.fee(61, 30));
        assertEquals(30, ParkingService.fee(0, 30)); // minimum one hour
    }

    @Test void reserveRejectsOverlappingBooking() {
        when(vehicles.findByPlate("KA01")).thenReturn(Optional.of(vehicle(1)));
        when(slots.findById(1L)).thenReturn(Optional.of(slot(20)));
        overlapReturns(List.of(new Reservation()));
        var e = assertThrows(ResponseStatusException.class, () -> svc.reserve("ka01", 1L, t, t.plusHours(2)));
        assertEquals(HttpStatus.CONFLICT, statusOf(e));
        verify(reservations, never()).save(any());
    }

    @Test void reserveRejectsEndBeforeStart() {
        var e = assertThrows(ResponseStatusException.class, () -> svc.reserve("KA01", 1L, t, t.minusHours(1)));
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(e));
    }

    @Test void reserveSavesWhenSlotFree() {
        when(vehicles.findByPlate("KA01")).thenReturn(Optional.of(vehicle(1)));
        when(slots.findById(1L)).thenReturn(Optional.of(slot(20)));
        overlapReturns(List.of());
        when(reservations.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        Reservation r = svc.reserve("KA01", 1L, t, t.plusHours(1));
        assertEquals(Reservation.Status.ACTIVE, r.getStatus());
    }

    @Test void entryBlockedWhenSlotReservedByAnotherVehicle() {
        when(vehicles.findByPlate("KA01")).thenReturn(Optional.of(vehicle(1)));
        when(slots.findById(1L)).thenReturn(Optional.of(slot(20)));
        Reservation other = new Reservation(); other.setVehicle(vehicle(2));
        overlapReturns(List.of(other));
        var e = assertThrows(ResponseStatusException.class, () -> svc.entry("KA01", 1L));
        assertEquals(HttpStatus.CONFLICT, statusOf(e));
    }

    @Test void exitChargesLocationRateAndFreesSlot() {
        ParkingRecord rec = new ParkingRecord();
        Slot s = slot(30); s.setStatus(Slot.Status.OCCUPIED);
        rec.setSlot(s); rec.setVehicle(vehicle(1)); rec.setEntryTime(LocalDateTime.now().minusMinutes(90));
        when(records.findFirstByVehiclePlateAndExitTimeIsNull("KA01")).thenReturn(Optional.of(rec));
        ParkingRecord out = svc.exit("KA01");
        assertEquals(60.0, out.getFee()); // 90 min -> 2 started hours x 30
        assertEquals(Slot.Status.AVAILABLE, s.getStatus());
    }

    @Test void registerRejectsDuplicatePlate() {
        when(vehicles.findByPlate("KA01")).thenReturn(Optional.of(vehicle(1)));
        Vehicle v = new Vehicle(); v.setPlate(" ka01 ");
        var e = assertThrows(ResponseStatusException.class, () -> svc.register(v));
        assertEquals(HttpStatus.CONFLICT, statusOf(e));
    }
}
