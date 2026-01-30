package com.telegram.service;

import com.telegram.config.BarbershopConfig;
import com.telegram.entity.Booking.BookingStatus;
import com.telegram.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository bookingRepository;
    private final BarbershopConfig config;

    public List<LocalDate> getAvailableDates() {
        List<LocalDate> availableDates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        log.info("Getting available dates. Today: {}, Current time: {}", today, currentTime);

        for (int i = 0; i < config.getBookingWindowDays(); i++) {
            LocalDate date = today.plusDays(i);

            // Skip closed days
            if (date.getDayOfWeek() == config.getClosedDays()) {
                log.debug("Skipping {} - closed day", date);
                continue;
            }

            // For today: check if currentTime + 2 hours < closingTime
            if (date.equals(today)) {
                LocalTime earliestBookableTime = currentTime.plusHours(config.getMinAdvanceBookingHours());

                // If earliest bookable time >= closing time, skip today
                if (!earliestBookableTime.isBefore(config.getClosingTime())) {
                    log.debug("Skipping today {} - too late (current: {}, earliest: {}, closing: {})",
                            date, currentTime, earliestBookableTime, config.getClosingTime());
                    continue;
                }
            }

            // Only add if there are available slots
            if (hasAvailableSlots(date)) {
                log.debug("Adding date {} - has available slots", date);
                availableDates.add(date);
            } else {
                log.debug("Skipping {} - no available slots", date);
            }
        }

        log.info("Found {} available dates", availableDates.size());
        return availableDates;
    }

    public List<LocalTime> getAvailableTimeSlots(LocalDate date) {
        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime closingTime = config.getClosingTime();
        int slotInterval = config.getSlotIntervalMinutes();

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        // If date is in the past, return empty
        if (date.isBefore(today)) {
            log.debug("Date {} is in the past, returning empty slots", date);
            return availableSlots;
        }

        // Determine the first available slot
        LocalTime firstSlot;
        if (date.equals(today)) {
            // For today: first slot is currentTime + 2 hours, rounded up to next slot
            LocalTime earliestTime = currentTime.plusHours(config.getMinAdvanceBookingHours());
            firstSlot = roundUpToNextSlot(earliestTime, config.getOpeningTime(), slotInterval);
        } else {
            // For future dates: start from opening time
            firstSlot = config.getOpeningTime();
        }

        // Iterate through slots
        LocalTime slot = firstSlot;
        while (slot.isBefore(closingTime)) {
            // Check if slot is already booked
            if (!isSlotBooked(date, slot)) {
                availableSlots.add(slot);
            }
            slot = slot.plusMinutes(slotInterval);
        }

        return availableSlots;
    }

    private LocalTime roundUpToNextSlot(LocalTime time, LocalTime openingTime, int slotInterval) {
        // If time is before opening, return opening time
        if (time.isBefore(openingTime)) {
            return openingTime;
        }

        // Round up to next slot interval
        int minutesSinceOpening = (time.getHour() * 60 + time.getMinute()) -
                (openingTime.getHour() * 60 + openingTime.getMinute());
        int slotsNeeded = (int) Math.ceil((double) minutesSinceOpening / slotInterval);
        return openingTime.plusMinutes((long) slotsNeeded * slotInterval);
    }

    public boolean isSlotBooked(LocalDate date, LocalTime time) {
        boolean confirmedExists = bookingRepository.existsByBookingDateAndStartTimeAndStatus(
                date, time, BookingStatus.CONFIRMED);
        boolean pendingExists = bookingRepository.existsByBookingDateAndStartTimeAndStatus(
                date, time, BookingStatus.PENDING);
        return confirmedExists || pendingExists;
    }

    private boolean hasAvailableSlots(LocalDate date) {
        return !getAvailableTimeSlots(date).isEmpty();
    }
}
